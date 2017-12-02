# Publish

1. Create `gradle.properties`, containing:
```
signing.keyId=[ID of the GPG key]
signing.password=[Password of the GPG key]
signing.secretKeyRingFile=[Location of the secret key ring file, e.g. /home/moe/.gnupg/secring.gpg]
sonatype.username=[Sonatype username]
sonatype.password=[Sonatype password]
```
1. Update version in `build.gradle`
1. Update version in `CHANGELOG.md`
1. Update version in `README.md`
1. Merge `develop` in `master`
1. Tag current version in format `v[version]`
1. Run `./gradlew clean build uploadArchives`
1. Open `https://oss.sonatype.org/#stagingRepositories`
1. Find the aleksa staging repo
1. Close the aleksa staging repo
1. Wait some time, then release the staging repo
