@echo off

javac -cp ..\..\java-advanced-2023\modules\info.kgeorgiy.java.advanced.implementor;..\java-solutions ^
      -d out ^
      ..\java-solutions\info\kgeorgiy\ja\belousov\implementor\Implementor.java && ^
jar --create --manifest MANIFEST.MF --file Implementor.jar -C out . && ^
echo Success

SET RESPONSE=Y
SET /P RESPONSE="Cleanup? [Y/n] "
IF /I "%RESPONSE%" == "n" GOTO end

rmdir out /s /q

:end
