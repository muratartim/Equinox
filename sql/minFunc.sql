CREATE FUNCTION MINIMUM(ARG1 DOUBLE, ARG2 DOUBLE) RETURNS DOUBLE
PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
EXTERNAL NAME 'java.lang.Math.min'