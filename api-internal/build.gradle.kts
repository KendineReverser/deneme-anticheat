plugins {
    deneme.`base-conventions`
}

dependencies {
    api(project(":api"))

    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.sqlite.jdbc)
    compileOnly(libs.mysql.jdbc)
    compileOnly(libs.postgres.jdbc)
    compileOnly(libs.mongoDriverSync)
    compileOnly(libs.jedis)
    compileOnly(libs.hikaricp)
    api(libs.disruptor)
}
