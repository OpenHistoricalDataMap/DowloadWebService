# Developer Starting Guide for the OHDM-Download-Service

### Introduction

Hi, my name is Note (Tino Geißler) and I'm one of the People who worked on this part of the OHDM Project for about one Semester now. I wasn't really the first one and I probably wont be the last (at least I hope so ^^). 

(This Service was originally created [here](https://github.com/parafoxx/ohdm-maps) (in Python). My Job was, to rewrite it in Java and put it though further development)

I really had fun working on on this and I hope you will too. To make it easier getting into this Project, maybe answering some questions you maybe have and showing some tough processes I put into this, I wanted to write a **Starting Guide** for people which want to work further on this. 

**All the Documentation can be found in the Wiki**

If you got any questions you want to have answered, as long as I'm still a Student at the HTW Berlin, my E-Mail is
_[Tino.Geissler@student.htw-berlin.de](mailto:Tino.Geissler@student.htw-berlin.de)_ . 

---

### What is this ?

To Start, let's answer the question : __What even is this?__

This Program is the "Server Part" of the Android App : [OHDM Offline Viewer](https://github.com/OpenHistoricalDataMap/OHDMOfflineViewer)



The 3 __Main__ Services are:

+ Receiving Requests from the App or a Website to "create" a Map of a specific size from a specific point in time
+ Using the [OHDMConverter](https://github.com/OpenHistoricalDataMap/OSMImportUpdate) and [osmosis](https://wiki.openstreetmap.org/wiki/Osmosis) to "build" a map-File from the request, and...
+ Distributing the map-File to the App or the Website



For these Services, I decided to use:

* [Spring Boot](https://spring.io/projects/spring-boot) (for "Request Handling" and "Status Distribution")
* [Apache MINA](https://mina.apache.org/mina-project/) (SFTP for the Distribution of said map-files)
* OHDMConverter and osmosis (for the "building" of the map file)
* and Java as the Main Programming Language



The Program itself is "module based". 
*(Which means it's build out of specific Modules which where made to work independently from one another, so they can be restarted/changed/swapped/etc. without damaging the rest of the System)*

These Modules are:

+ [The Request Manager](https://github.com/OpenHistoricalDataMap/DownloadWebService/wiki/The-Request-Manager)

  + Manager for the Query Requests, can start and stop ongoing "Requests". 

  + manages the number of Requests, that are allowed to run at the same time and automatically starts new, if they are allowed

  + is it's own Thread

    

+ [The Web Service](https://github.com/OpenHistoricalDataMap/DownloadWebService/wiki/Main-Class-:-SpringClass) (Spring Boot)

  + just takes HTTP Requests and processes them

  + in the Main Class (Spring Class) 

    

+ [The SFTP File Service](https://github.com/OpenHistoricalDataMap/DownloadWebService/wiki/SFTP-File-Service) (Apache MINA)

  + used to create an SFTP Server

  + is it's own Thread

     

+ [The ID Management "System"](https://github.com/OpenHistoricalDataMap/DownloadWebService/wiki/ID-System-Module)

  + Just saves/creates/deletes IDs 

  + not a Thread

    

+ [The Query Requests](https://github.com/OpenHistoricalDataMap/DownloadWebService/wiki/The-Request-Class)

  + is an Object

  + specifically build for downloading, creating and converting map files from a Database

  + is it's own Thread

  + is/are managed by the Request Manager

    

+ [The "Static Variables Initializer"](https://github.com/OpenHistoricalDataMap/DownloadWebService/wiki/init.txt-configuration)

  + Initializes Variables from a File

  + is used for initializing the entire System

  + not a Thread

    

+ [The Logger](https://github.com/OpenHistoricalDataMap/DownloadWebService/wiki/Logger-Module)

  + A small Logger System with **re-usability** in mind

  + is it's own Thread

    

+ [The Controller Endpoint](https://github.com/OpenHistoricalDataMap/DownloadWebService/wiki/The-Controller-Endpoint)

  + Access-point for the Service Controller
  + is it's own Thread 



*__Authors Note__: I've tried to build every single Module with the Idea in mind, that you can just take one out and even run it Standalone. This didn't always work out, but with a little bit of tweaking, I bet these things can be reused anywhere. Feel free to try it out yourself.*

 

### Footer
OHDM Server with map download service for Andropid App
Wiki : https://github.com/OpenHistoricalDataMap/DowloadWebService/wiki

Download Web Service for the Android App :
https://github.com/OpenHistoricalDataMap/OHDMOfflineViewer
