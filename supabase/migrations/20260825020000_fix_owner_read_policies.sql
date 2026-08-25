-- Fix: owners must be able to see rows in their own playlists.
-- The reconstructed read policies only checked membership, so ON CONFLICT
-- DO UPDATE failed with 42501 (existing row invisible to the owner) and
-- song/member pulls returned empty for playlist owners.

drop policy if exists "songs_read" on public.hika_playlist_songs;
create policy "songs_read" on public.hika_playlist_songs
  for select to authenticated
  using (public.hika_is_member(playlist_id) or public.hika_is_owner(playlist_id));

drop policy if exists "members_read" on public.hika_playlist_members;
create policy "members_read" on public.hika_playlist_members
  for select to authenticated
  using (public.hika_is_member(playlist_id) or public.hika_is_owner(playlist_id));
