whats-up-next-plugin
====================

This plugin adds a view which list all the jobs (access rights respected) which have a timer trigger or SCM polling. 
The user can see when the next scheuled run or polling will occur.

Note: Uses quartz scheudler which doesn't understand if both day of month and day of week is specified. In those cases Day of month "wins" 
and day of week is ignored in the calculation.
