aips2xml
========

aips2xml - sanitize AIPS XML 

##Usage
java -jar aips2xml.jar

##Generate sanitized (German) HTML and XML files
java -jar aips2xml.jar -lang=de

dito French:
java -jar aips2xml.jar -lang=fr

All files can then be found in the "fis" Folder.

##Help
```
java -jar aips2xml.jar --help
usage: aips2xml
    --alpha <arg>   only include titles which start with option value
    --help          print this message
    --lang <arg>    use given language
    --nodown        no download, parse only
    --quiet         be extra quiet
    --verbose       be extra verbose
    --version       print the version information and exit
    --zip           generate zip file
```
