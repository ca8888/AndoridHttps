# AndoridHttps
An exmaple file for showing the following:
1. andorid https request using AsyncTask
2. support defualt certificate
3. support self sign certificate.

## certificate notes:
for creating a cartificate in the server side please run the cmd:
```
openssl req -x509 -newkey rsa:4096 -nodes -out cert.pem -keyout key.pem -days 365

```
*when running the cmd a few question would be showed:

```
Country Name (2 letter code) [AU]:US
State or Province Name (full name) [Some-State]:new york
Locality Name (eg, city) []:new york
Organization Name (eg, company) [Internet Widgits Pty Ltd]:
Organizational Unit Name (eg, section) []:
Common Name (e.g. server FQDN or YOUR name) []:<your ip!!!>
Email Address []:
```
pay great attention to the Common Name that is the only host your certificate would support@!

* andorid as client side:
andorid do not support .pem rather .cer 
for changing the cert.pem use 

```
openssl x509 -outform der -in cert.pem  -out cert.crt
```
* the cert should be placed in your main/assets/cert.crt fodler
