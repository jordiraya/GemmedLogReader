Lectura de logs dels hospitals ICS v.1.3
----------------------------------------

Codi font: https://github.com/jordiraya/GemmedLogReader


Els logs són arxius server.log.YYYY-MM-DD extrets de cada servidor, ubicats directori C:\Program Files\gemmed\ghl\jboss-4.2.3.GA\server\default\log
S'assumeix que dins aquests logs les línees amb "remoteAE: NOM_MAQUINA" indiquen connexions amb aquesta màquina

Utilització
1- Assegurar-se que està instal·lat de Java 8 o superior 
	java -version
2- Copiar els logs en un directori local, diferent per a cada hospital
3- Copiar LogReader.jar en un directori local
4- Cridar el lector de logs al jar des de la consola de comandes:
	java -jar LogReader.jar <mode> <pathLogs> <init date yyyy-mm-dd> <end date yyyy-mm-dd> [pathCSVExport]

mode indica el mode de funcionament
mode 1: llista els ECGs per pantalla
mode 2: llista els ECGs i el nombre de connexions per cada ECG
mode 3: escriu a un arxiu CSV una línia per ECG, data i nomnre de connexions, requereix el path de l'arxiu pathCSVExport

les dates inicial i final del rang de dates s'han d'escriure en format yyyy-mm-dd

exemple per llistar:
java -jar LogReader.jar 1 c:\gemmed\verge_cinta 2022-01-01 2022-12-31

exemple per obtenir l'arxiu CSV
java -jar LogReader.jar 3 c:\gemmed\verge_cinta 2022-01-01 2022-12-31 c:\gemmed\analisi\verge_cinta_2023.csv
