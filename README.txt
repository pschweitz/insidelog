DBI Tail is a tool created by Philippe Schweitzer
https://ch.linkedin.com/pub/philippe-schweitzer/87/ab/2bb

Download available from this page: https://github.com/pschweitz/DBITail/releases

For Windows, run "dbiTail.bat"
For linux, run "dbiTail.sh"

It is released under Apache v2 License

/*
 * Copyright 2015 Philippe Schweitzer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

Please visit GitHub page:
https://github.com/pschweitz/DBITail


Requires java 8.0_60 or higher


If you experience some troubles or slowdowns, it is advised to setup Java max heap size to 1Go

     -> Just use dbiTail.bat or dbitail.sh
     -> Or create a shortcut and fill-in/adapt following properties to your environment:
              Target : C:\<path to java>\javaw.exe -Xmx1g -jar �C:\<path to DBI Tail>\dbi-Tail.jar" 
              Start in : C:\<path to DBI Tail> 


BEAWARE:

Run of DBI Tail on a production server is at your own risks. 
If you want to view log files from there, it is recommended 
to open a Read-Only shared folder over the network. 

