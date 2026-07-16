; Inno Setup script for Metrolist Desktop
; Requires Inno Setup 6+ (https://jrsoftware.org/isdl.php)

#define MyAppName "Metrolist"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "Metrolist"
#define MyAppURL "https://github.com/MetrolistGroup/Metrolist"
#define MyAppExeName "Metrolist.exe"

[Setup]
AppId={{3A8E4F2C-1D6B-4A9E-8C7F-5D2E3A1B0C4D}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
AllowNoIcons=yes
OutputDir=..\build\installer
OutputBaseFilename=Metrolist-Setup-v{#MyAppVersion}
Compression=lzma
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest
DisableProgramGroupPage=auto

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "indonesian"; MessagesFile: "compiler:Languages\Indonesian.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "..\build\compose\binaries\main\exe\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\build\compose\binaries\main\exe\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; VLC runtime (optional - user can install VLC separately)
; Source: "C:\Program Files\VideoLAN\VLC\*"; DestDir: "{app}\vlc"; Flags: ignoreversion recursesubdirs createallsubdirs; Check: VLCNotInstalled

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Code]
function VLCNotInstalled: Boolean;
begin
  Result := not RegKeyExists(HKLM, 'Software\VideoLAN\VLC');
end;
