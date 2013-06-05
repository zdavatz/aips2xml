aips2xml
========

aips2xml - sanitize AIPS XML 

##Requirements
Java 1.7 (tested on Linux, Mac, Windows)

##Usage
java -jar aips2xml.jar

##Generate sanitized (German) HTML and XML files
java -jar aips2xml.jar -lang=de

 [ on a decent PC with 8 cores, i7 it will take about 27 minutes to sanitize ca. 3900 FIs ]

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

If you have problem with Out-Of-Memory Errors you can try:

$ java -Xmx4096m -jar aips2xml.jar -lang=de

##Eclipse
Eclipse Users can open the project on Eclipse and hack away and send us a patch.
