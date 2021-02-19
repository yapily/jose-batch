| |Current Status|
|---|---|
|Build|[![Build Status](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Factions-badge.atrox.dev%2Fyapily%2Fjose-batch%2Fbadge%3Fref%3Dmaster&style=flat)](https://actions-badge.atrox.dev/yapily/jose-batch/goto?ref=master)|
|Docker Hub|[![](https://images.microbadger.com/badges/version/yapily/jose-batch.svg)](https://microbadger.com/images/yapily/jose-batch "Get your own version badge on microbadger.com")|
|License|![license](https://img.shields.io/github/license/yapily/jose-batch)|

# JOSE Batch

JOSE batch is an utility to keep your PostgreSQL database fields encrypted with the latest JWT field encryption keys. This is acheived by accessing a database table, decrypting fields using an expired key and re-encrypting them using a valid key.


It is based on one of our other open source contributions [https://github.com/yapily/jose-database](https://github.com/yapily/jose-database) which enables a Spring application to encrypt/decrypt specific fields in your database.

This utility is written in Java using Spring batch but is packaged as a Docker image, so you should be able to use it without worrying about the implementation language.

## The database field encryption format

The fields in your database must be formatted as JWTs. The batch utility support different format of JWTs:

- JWS: Json Web Signing
- JWE: Json Web Encrypting 
- JWE_JWS: A JWS that is then used as payload of a JWE. ie: JWE(JWS)
- JWS_JWE: a JWE that is then used as payload of a JWS. ie: JWS(JWE)

We recommend using the format JWS_JWE for the reason details here: [https://github.com/yapily/jose-database#should-i-always-use-jws_jwe-](https://github.com/yapily/jose-database#should-i-always-use-jws_jwe-)

## How to use the Docker image?

The Docker image will expect a set of keys to be mounted, as well as some environment variables.
You can find the latest Docker image on Docker Hub: https://hub.docker.com/r/yapily/jose-batch/

### Environment variables


- `jose-database.tokenFormat`: Specify the format for the encrypted fields: `JWS`, `JWE`, `JWE_JWS`, `JWS_JWE`
- `jose-batch.chunk-size`: Specify the number of entries for the batch utility to write in the database update query
- `jose-batch.page-size`: Specify the number of entries for the batch utility to read in the database read query
- `jose-batch.threads`: Define the number of threads in parallel for each chunk of jobs. Default is 1
- `jose-batch.table`: Specify the table name for the batch utility to use in the database
- `jose-batch.id`: Specify the ID column of your table. This is required for updating rows.
- `jose-batch.id-filter`: Specify the way you want to filter your id. For uuid, it needs to be `id::text`. This is required for updating rows.
- `jose-batch.fields`: Specify the list of the fields for the batch utility to re-encrypt
- `spring.datasource.url`: Specify the PostgreSQL datasource url used by Spring. Here is an example: `jdbc:postgresql://$YourDatabaseHostname:$YourDatabasePort/$YourDatabaseName`
- `spring.datasource.username`: Specify the Spring database username
- `spring.datasource.password`: Specify the Spring database password
- `jose-database.keys-path`: Specify the path to the mounted keys as JWK sets (see the "key format" section below)

Example:

```
env:
    - name: jose-database.tokenFormat
      value: "JWS_JWE"
    - name: jose-batch.chunk-size
      value: "100"
    - name: jose-batch.page-size
      value: "10"
    - name: jose-batch.threads
      value: "10"
    - name: jose-batch.table
      value: "$yourTableThatHasTheEncryptionField"
    - name: jose-batch.id
      value: "$YourTableIDFieldName"
    - name: jose-batch.id
      value: "$YourTableIDFieldName::text"
    - name: jose-batch.fields
      value: "$YourListOfEncryptedFieldNames"
    - name: spring.datasource.url
      value: jdbc:postgresql://$YourDatabaseHostname:$YourDatabasePort/$YourDatabaseName
    - name: spring.datasource.username
      value: $DatabaseUserName
    - name: spring.datasource.password
      valueFrom:
        secretKeyRef:
          name: database-password
          key: spring.r2dbc.password
    - name: jose-database.keys-path
      value: classpath:keys/
```

### Key Format

The batch utility expects three classifications of keys which are each formatted as a [JWK set](https://tools.ietf.org/html/rfc7517#section-5) following the [JWK standard](https://tools.ietf.org/html/rfc7517):
- `valid-keys`: the keys that are currently valid and should be used for any new entries
- `expired-keys`: the keys that are now expired but should be used to read old entries
- `revoked-keys`: the keys that are now revoked. They are not needed but for good practice, we also keep them to have a history of the keys.

Here is an example of each of the keys: https://github.com/yapily/jose-batch/tree/master/keys

Please follow the same file naming convention!

You don't need to create these files manually as we created another utility to manage these keys: [https://github.com/yapily/jose-cli](https://github.com/yapily/jose-cli)
The output of this CLI can be used as input of this JOSE batch utility.

### Mount the keys

You will need to mount the folder containing the 3 keys sets into the Docker image. This is not convered, but note that the path you choose needs to be specified in an env variable `jose-database.keys-path`


