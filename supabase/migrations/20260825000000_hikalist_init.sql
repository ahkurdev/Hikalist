-- Hikalist schema reconstruction
-- Rebuilt from the desktop client contract:
--   sync/HikalistSupabaseClient.kt, SupabasePlaylistSyncRemote.kt,
--   PlaylistCollaborationRepository.kt, auth/DesktopAccountProfileRepository.kt

create extension if not exists pgcrypto;

-- ============================================================
-- Shared change sequence (cursor-based incremental pull)
-- ============================================================
create sequence if not exists hika_change_seq start 1;

create or replace function hika_bump_change_seq()
returns trigger
language plpgsql
as $$
begin
  new.change_seq := nextval('hika_change_seq');
  return new;
end;
$$;

-- ============================================================
-- Profiles
-- ============================================================
create table if not exists public.hika_profiles (
  user_id    uuid primary key references auth.users(id) on delete cascade,
  username   text not null,
  avatar_url text,
  email      text,
  created_at timestamptz not null default now()
);

-- ============================================================
-- Playlists
-- ============================================================
create table if not exists public.hika_playlists (
  id          text primary key,
  owner_id    uuid not null references auth.users(id) on delete cascade,
  name        text not null default '',
  description text not null default '',
  cover_url   text,
  deleted_at  timestamptz,
  change_seq  bigint not null default 0,
  created_at  timestamptz not null default now()
);

-- ============================================================
-- Playlist songs
-- ============================================================
create table if not exists public.hika_playlist_songs (
  playlist_id   text not null references public.hika_playlists(id) on delete cascade,
  song_id       text not null,
  title         text not null default '',
  artist        text not null default '',
  album         text,
  thumbnail_url text,
  duration      int  not null default 0,
  position      int  not null default 0,
  added_by      uuid not null,
  added_by_name text not null default '',
  added_at      timestamptz not null default now(),
  deleted_at    timestamptz,
  change_seq    bigint not null default 0,
  primary key (playlist_id, song_id)
);

-- ============================================================
-- Playlist members (collaboration)
-- ============================================================
create table if not exists public.hika_playlist_members (
  playlist_id text not null references public.hika_playlists(id) on delete cascade,
  user_id     uuid not null references auth.users(id) on delete cascade,
  role        text not null default 'editor' check (role in ('owner', 'editor')),
  deleted_at  timestamptz,
  change_seq  bigint not null default 0,
  primary key (playlist_id, user_id)
);

-- ============================================================
-- Invite links (accessed only through RPCs below)
-- ============================================================
create table if not exists public.hika_playlist_invites (
  token       text primary key,
  playlist_id text not null,
  created_by  uuid not null references auth.users(id) on delete cascade,
  created_at  timestamptz not null default now(),
  expires_at  timestamptz not null,
  revoked_at  timestamptz
);

-- ============================================================
-- change_seq triggers
-- ============================================================
drop trigger if exists hika_playlists_bump on public.hika_playlists;
create trigger hika_playlists_bump
  before insert or update on public.hika_playlists
  for each row execute function hika_bump_change_seq();

drop trigger if exists hika_playlist_songs_bump on public.hika_playlist_songs;
create trigger hika_playlist_songs_bump
  before insert or update on public.hika_playlist_songs
  for each row execute function hika_bump_change_seq();

drop trigger if exists hika_playlist_members_bump on public.hika_playlist_members;
create trigger hika_playlist_members_bump
  before insert or update on public.hika_playlist_members
  for each row execute function hika_bump_change_seq();

-- ============================================================
-- Indexes
-- ============================================================
create index if not exists hika_playlists_change_idx     on public.hika_playlists (change_seq);
create index if not exists hika_playlists_owner_idx      on public.hika_playlists (owner_id);
create index if not exists hika_songs_change_idx         on public.hika_playlist_songs (change_seq);
create index if not exists hika_songs_playlist_idx       on public.hika_playlist_songs (playlist_id);
create index if not exists hika_members_change_idx       on public.hika_playlist_members (change_seq);
create index if not exists hika_members_playlist_idx     on public.hika_playlist_members (playlist_id);
create index if not exists hika_invites_playlist_idx     on public.hika_playlist_invites (playlist_id);

-- ============================================================
-- Access helpers (security definer avoids recursive RLS)
-- ============================================================
create or replace function public.hika_is_member(p_playlist_id text)
returns boolean
language sql stable security definer set search_path = public
as $$
  select exists (
    select 1 from hika_playlist_members m
    where m.playlist_id = p_playlist_id
      and m.user_id = auth.uid()
      and m.deleted_at is null
  );
$$;

create or replace function public.hika_is_owner(p_playlist_id text)
returns boolean
language sql stable security definer set search_path = public
as $$
  select exists (
    select 1 from hika_playlists p
    where p.id = p_playlist_id
      and p.owner_id = auth.uid()
  );
$$;

create or replace function public.hika_can_edit_songs(p_playlist_id text)
returns boolean
language sql stable security definer set search_path = public
as $$
  select public.hika_is_owner(p_playlist_id)
      or exists (
        select 1 from hika_playlist_members m
        where m.playlist_id = p_playlist_id
          and m.user_id = auth.uid()
          and m.deleted_at is null
          and m.role = 'editor'
      );
$$;

-- ============================================================
-- Row Level Security
-- ============================================================
alter table public.hika_profiles        enable row level security;
alter table public.hika_playlists       enable row level security;
alter table public.hika_playlist_songs  enable row level security;
alter table public.hika_playlist_members enable row level security;
alter table public.hika_playlist_invites enable row level security;

-- profiles: readable by any authenticated user (username/avatar lookup),
-- writable only by the owner of the row
drop policy if exists "profiles_read" on public.hika_profiles;
create policy "profiles_read" on public.hika_profiles
  for select to authenticated using (true);

drop policy if exists "profiles_insert_own" on public.hika_profiles;
create policy "profiles_insert_own" on public.hika_profiles
  for insert to authenticated with check (user_id = auth.uid());

drop policy if exists "profiles_update_own" on public.hika_profiles;
create policy "profiles_update_own" on public.hika_profiles
  for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- playlists: visible to owner and active members; mutated by owner only
drop policy if exists "playlists_read" on public.hika_playlists;
create policy "playlists_read" on public.hika_playlists
  for select to authenticated
  using (owner_id = auth.uid() or public.hika_is_member(id));

drop policy if exists "playlists_insert_own" on public.hika_playlists;
create policy "playlists_insert_own" on public.hika_playlists
  for insert to authenticated with check (owner_id = auth.uid());

drop policy if exists "playlists_update_own" on public.hika_playlists;
create policy "playlists_update_own" on public.hika_playlists
  for update to authenticated
  using (owner_id = auth.uid())
  with check (owner_id = auth.uid());

-- songs: visible to members; editable by owner/editors
drop policy if exists "songs_read" on public.hika_playlist_songs;
create policy "songs_read" on public.hika_playlist_songs
  for select to authenticated
  using (public.hika_is_member(playlist_id));

drop policy if exists "songs_insert" on public.hika_playlist_songs;
create policy "songs_insert" on public.hika_playlist_songs
  for insert to authenticated
  with check (public.hika_can_edit_songs(playlist_id));

drop policy if exists "songs_update" on public.hika_playlist_songs;
create policy "songs_update" on public.hika_playlist_songs
  for update to authenticated
  using (public.hika_can_edit_songs(playlist_id))
  with check (public.hika_can_edit_songs(playlist_id));

drop policy if exists "songs_delete" on public.hika_playlist_songs;
create policy "songs_delete" on public.hika_playlist_songs
  for delete to authenticated
  using (public.hika_can_edit_songs(playlist_id));

-- members: visible to members; managed by owner only
drop policy if exists "members_read" on public.hika_playlist_members;
create policy "members_read" on public.hika_playlist_members
  for select to authenticated
  using (public.hika_is_member(playlist_id));

drop policy if exists "members_insert" on public.hika_playlist_members;
create policy "members_insert" on public.hika_playlist_members
  for insert to authenticated
  with check (public.hika_is_owner(playlist_id));

drop policy if exists "members_update" on public.hika_playlist_members;
create policy "members_update" on public.hika_playlist_members
  for update to authenticated
  using (public.hika_is_owner(playlist_id))
  with check (public.hika_is_owner(playlist_id));

drop policy if exists "members_delete" on public.hika_playlist_members;
create policy "members_delete" on public.hika_playlist_members
  for delete to authenticated
  using (public.hika_is_owner(playlist_id));

-- invites: no policies -> reachable only via security definer RPCs

-- ============================================================
-- Invite RPCs
-- ============================================================
create or replace function public.create_hika_playlist_invite(
  p_playlist_id text,
  p_ttl_seconds int
)
returns table (token text, expires_at timestamptz)
language plpgsql volatile security definer set search_path = public
as $$
declare
  v_token    text;
  v_expires  timestamptz;
  v_owner    uuid;
begin
  select owner_id into v_owner from hika_playlists where id = p_playlist_id;
  if v_owner is null then
    raise exception 'Playlist is not available';
  end if;
  if v_owner <> auth.uid() then
    raise exception 'Only the playlist owner can create invite links';
  end if;

  v_token   := encode(gen_random_bytes(32), 'hex');
  v_expires := now() + make_interval(secs => greatest(coalesce(p_ttl_seconds, 7200), 60));

  insert into hika_playlist_invites (token, playlist_id, created_by, expires_at)
  values (v_token, p_playlist_id, auth.uid(), v_expires);

  return query select v_token, v_expires;
end;
$$;

create or replace function public.revoke_hika_playlist_invites(
  p_playlist_id text
)
returns void
language plpgsql volatile security definer set search_path = public
as $$
begin
  if not public.hika_is_owner(p_playlist_id) then
    raise exception 'Only the playlist owner can revoke invite links';
  end if;

  update hika_playlist_invites
  set revoked_at = now()
  where playlist_id = p_playlist_id
    and revoked_at is null
    and expires_at > now();
end;
$$;

grant execute on function public.create_hika_playlist_invite(text, int) to authenticated;
grant execute on function public.revoke_hika_playlist_invites(text) to authenticated;

-- ============================================================
-- Storage buckets: avatars + playlist-covers (public read)
-- ============================================================
insert into storage.buckets (id, name, public)
values
  ('avatars', 'avatars', true),
  ('playlist-covers', 'playlist-covers', true)
on conflict (id) do nothing;

drop policy if exists "avatars_public_read" on storage.objects;
create policy "avatars_public_read" on storage.objects
  for select using (bucket_id = 'avatars');

drop policy if exists "avatars_user_write" on storage.objects;
create policy "avatars_user_write" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'avatars'
              and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "avatars_user_update" on storage.objects;
create policy "avatars_user_update" on storage.objects
  for update to authenticated
  using (bucket_id = 'avatars'
         and (storage.foldername(name))[1] = auth.uid()::text)
  with check (bucket_id = 'avatars'
              and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "avatars_user_delete" on storage.objects;
create policy "avatars_user_delete" on storage.objects
  for delete to authenticated
  using (bucket_id = 'avatars'
         and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "covers_public_read" on storage.objects;
create policy "covers_public_read" on storage.objects
  for select using (bucket_id = 'playlist-covers');

drop policy if exists "covers_user_write" on storage.objects;
create policy "covers_user_write" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'playlist-covers'
              and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "covers_user_update" on storage.objects;
create policy "covers_user_update" on storage.objects
  for update to authenticated
  using (bucket_id = 'playlist-covers'
         and (storage.foldername(name))[1] = auth.uid()::text)
  with check (bucket_id = 'playlist-covers'
              and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "covers_user_delete" on storage.objects;
create policy "covers_user_delete" on storage.objects
  for delete to authenticated
  using (bucket_id = 'playlist-covers'
         and (storage.foldername(name))[1] = auth.uid()::text);

-- ============================================================
-- Realtime: broadcast changes on sync tables
-- ============================================================
do $$
begin
  alter publication supabase_realtime add table public.hika_playlists;
exception when duplicate_object then null;
end $$;

do $$
begin
  alter publication supabase_realtime add table public.hika_playlist_songs;
exception when duplicate_object then null;
end $$;

do $$
begin
  alter publication supabase_realtime add table public.hika_playlist_members;
exception when duplicate_object then null;
end $$;
