| |Current Status|
|---|---|
|Build|[![Build Status](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Factions-badge.atrox.dev%2Fyapily%2Fjose-batch%2Fbadge%3Fref%3Dmaster&style=flat)](https://actions-badge.atrox.dev/yapily/jose-batch/goto?ref=master)|
|Docker Hub|[![](https://images.microbadger.com/badges/version/yapily/jose-batch.svg)](https://microbadger.com/images/yapily/jose-batch "Get your own version badge on microbadger.com")|
|License|![license](https://img.shields.io/github/license/yapily/jose-batch)|

# JOSE Batch

JOSE batch is an utility to maintain your PostgreSQL database in a good state, when you use JWT fields encryption.

The goal of this batch is to go through a database table, to decrypt a column using some expired keys and re-encrypt them using some valid keys.


It is based on one of our other open source contribution [https://github.com/yapily/jose-database](https://github.com/yapily/jose-database) which offer to your Spring 
application to encrypt/decrypt a specific field of your database.

This utility is write in Java using Spring batch. Although as the artefacts produce is a docker image, you should be able to consume the docker
image without worrying about the implementation language.

## The database fields encryption format

The field of your database must be formatted as JWT. The batch offers to support different format of JWTS:

- JWS
- JWE
- JWE_JWS : A JWS that is then used as payload of a JWE. ie: JWE(JWS)
- JWS_JWE : a JWE that is then used as payload of a JWS. ie: JWS(JWE)

We recommend using the format JWS_JWE for the reason details here: [https://github.com/yapily/jose-database#should-i-always-use-jws_jwe-](https://github.com/yapily/jose-database#should-i-always-use-jws_jwe-)

## How to use the docker image?

The docker image will expect to have mounted a set of keys, as well as some environment variables.
You can find the latest docker image on docker hub: https://hub.docker.com/r/yapily/jose-batch/

### Environment variables


- `jose-database.tokenFormat`: Specify the type of format you field. `JWS`, `JWE`, `JWE_JWS`, `JWS_JWE`
- `jose-batch.chunk-size`: How many entries you want the batch to write by database update query
- `jose-batch.page-size`: How many entries you want the batch to read by database read query
- `jose-batch.id`: The batch needs to know the column use as ID for this table
- `jose-batch.fields`: The list of the fields that you want the batch to re-encrypt
- `spring.datasource.url`: The PostgreSQL datasource url use by Spring. Here is an example: `jdbc:postgresql://$YourDatabaseHostname:$YourDatabasePort/$YourDatabaseName`
- `spring.datasource.username`: The database username
- `spring.datasource.password`: The database password
- `jose-database.keys-path`: the path where you mounted the keys as JWK set (see section around key format expected)

Example:

```
env:
    - name: jose-database.tokenFormat
      value: "JWS_JWE"
    - name: jose-batch.chunk-size
      value: "100"
    - name: jose-batch.page-size
      value: "10"
    - name: jose-batch.table
      value: "$yourTableThatHasTheEncryptionField"
    - name: jose-batch.id
      value: "$YourTableIDFieldName"
    - name: jose-batch.fields
      value: "$YourEncryptedFieldName"
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

### Format of the keys

The batch is expecting the keys to be format as JWK set, and categorised in three different status:
- `valid-keys`: the keys that are currently valid and should be used for any new entries
- `expired-keys`: the keys that are now expired but should be used to read old entries
- `revoked-keys`: the keys that are now revoked. They are not needed but by good practice, we also keep them to not loose the history.

The keys follow the [JWK standard](https://tools.ietf.org/html/rfc7517) and a set of keys would be cover on [section 5](https://tools.ietf.org/html/rfc7517#section-5).
You got an example of keys in here: https://github.com/yapily/jose-batch/tree/master/keys

Please follow the same file name convention!

You don't need to create those files manually, we actually created another utility to manage those keys: [https://github.com/yapily/jose-cli](https://github.com/yapily/jose-cli)
The output of this CLI can be used as input of this JOSE batch utility.

### Mount the keys

You will need to mount the folder containing the 3 keys sets into the docker image. We would not cover how to do so, just note that the path you choose needs to be specified in an env variable `jose-database.keys-path`


