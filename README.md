## Insidelog is a colirized real-time log viewer tool made in Java
Created by [Philippe Schweitzer](https://ch.linkedin.com/pub/philippe-schweitzer/87/ab/2bb)

![alt text](https://github.com/pschweitz/insidelog/blob/master/InSideLog.png)

## Find the user manual here: 
https://github.com/pschweitz/insidelog/blob/master/UserManual.pdf 

## Downloads are available in this page: 
https://github.com/pschweitz/insidelog/releases

You can find packaged builds with embedded Java, so run of software will be very straightforward.

Just choose the one for your operating system and architecture, then: 

For Windows, run "insidelog.bat"
For linux, run "insidelog.sh"

Sorry for Mac users, packaged build is not available for the moment.

    It is released under Apache v2 License
    
    Copyright 2022 Philippe Schweitzer.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

Please visit GitHub page:
https://github.com/pschweitz/insidelog


Requires java 8.0_60 or higher


If you experience some troubles or slowdowns, it is advised to setup Java max heap size to 1Go

    -> Just use insidelog.bat or insidelog.sh
    -> Or create a shortcut and fill-in/adapt following properties to your environment:
              Target : C:\<path to java>\javaw.exe -Xmx1g -jar ?C:\<path to insidelog>\insidelog.jar" 
              Start in : C:\<path to insidelog> 


## BEAWARE:

Run of insidelog on a production server is at your own risks. 
If you want to view log files from there, it is recommended 
to open a Read-Only shared folder over the network, or to use SSH. 

