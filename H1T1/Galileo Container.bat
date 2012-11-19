@echo off
set jade=C:\JADE\bin\lib\jade.jar
set agentclasses=C:\Users\tolkjen\git\H1\H1T1\bin
 
set classes=%jade%;%agentclasses%
set agents=-agents GalileoManager:ArtistManagerAgent(GalileoCurator)
set name=GalileoContainer
 
cd C:/JADE
java -cp %classes% jade.Boot -container -container-name %name% %agents%