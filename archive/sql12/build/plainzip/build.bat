set PLAIN_ZIP_DIR=c:\projects\squirrel-sql\sql12\build\plainzip

set INSTALL_JAR=c:\tmp\squirrel-sql-2.6.3-install.jar

set VERSION=2.6.3

cd %PLAIN_ZIP_DIR%

del /Q *.zip
del /Q tmp
mkdir tmp

java -jar %INSTALL_JAR% auto_install_base.xml

cd tmp

jar -cvf squirrel-sql-%VERSION%-base.zip "SQuirreL SQL Client"

move squirrel-sql-%VERSION%-base.zip ..\

cd ..

del /Q -rf tmp
mkdir tmp


java -jar %INSTALL_JAR% auto_install_standard.xml

cd tmp

jar -cvf squirrel-sql-%VERSION%-standard.zip "SQuirreL SQL Client"

move squirrel-sql-%VERSION%-standard.zip ..\

cd ..

del /Q tmp
mkdir tmp

java -jar %INSTALL_JAR% auto_install_optional.xml

cd tmp

jar -cvf squirrel-sql-%VERSION%-optional.zip "SQuirreL SQL Client"

move squirrel-sql-%VERSION%-optional.zip ..\

cd ..

del /Q tmp
