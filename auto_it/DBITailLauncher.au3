$ver = FileGetVersion("java.exe")
if $ver == "0.0.0.0" Then
   MsgBox(0, "No Java detected", "Install Java Runtime Enviroment")
Else
   ShellExecute('insidelog.jar', '', @WorkingDir)
EndIf