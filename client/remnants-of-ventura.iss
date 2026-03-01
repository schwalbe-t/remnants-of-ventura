
#if Version == ""
    #error "Version ('Version') cannot be empty."
#endif

#if SrcDir == ""
    #error "Source directory ('SrcDir') cannot be empty."
#endif

#define MyAppName "Remnants of Ventura"
#define MyAppPublisher "schwalbe_t"
#define MyAppURL "https://www.github.com/schwalbe-t/remnants-of-ventura"
#define MyAppExeName "run.bat"
#define MyAppSrcDir SrcDir
#define MyAppVersion Version

[Setup]
AppId={{1560c8a7-e1c7-49d3-be17-dc560e9dedf0}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={userappdata}\.remnants-of-ventura
UninstallDisplayIcon={app}\res\icon.ico
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
DisableProgramGroupPage=yes
DisableDirPage=yes
LicenseFile={#MyAppSrcDir}\LICENSE
PrivilegesRequired=lowest
OutputBaseFilename=remnants-of-ventura-{#MyAppVersion}
SetupIconFile={#MyAppSrcDir}\res\icon.ico
SolidCompression=yes
WizardStyle=modern
WizardImageFile={#MyAppSrcDir}\res\installer_banner.png
WizardSmallImageFile={#MyAppSrcDir}\res\icon.png

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "bulgarian"; MessagesFile: "compiler:Languages\Bulgarian.isl"
Name: "german"; MessagesFile: "compiler:Languages\German.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#MyAppSrcDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\res\icon.ico"
Name: "{userdesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\res\icon.ico"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: files; Name: "{app}\config.json"
Type: files; Name: "{app}\latest.log"
Type: files; Name: "{app}\usercode\*"
