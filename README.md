# Privoxy

Privoxy is a system that enforces privacy policies for web applications.  It
interposes as a proxy between a web server and its backend database, dynamically
verifies that queries issued by the web server comply with data protection
policies, and rejects non-compliant queries.

A privacy policy in Privoxy consists of database view definitions (i.e., SQL
`SELECT` statement) that define what information in the database is accessible
when serving a web request;
the view definitions may refer to request-specific parameters supplied by the
web application (e.g., the current user ID).
Here is an [example policy file](src/test/resources/DiasporaTest/policies.sql).
Given such a view-based privacy policy, Privoxy ensures at run-time that any
application-issued SQL query reveals only information covered by the views.

Our current prototype is implemented as a JDBC driver and thus only works with
JVM-based applications (although its design does not preclude a standalone SQL
proxy implementation).  See [here](src/test/java/client/DiasporaTest.java)
for a usage example based on the [Diaspora](https://diasporafoundation.org/)
application.

Privoxy is being developed at the [Berkeley NetSys Lab](https://netsys.cs.berkeley.edu/).
If you have any questions, please contact Wen Zhang at zhangwen@cs.berkeley.edu.
