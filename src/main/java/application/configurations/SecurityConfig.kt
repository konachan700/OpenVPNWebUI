package application.configurations

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.security.core.userdetails.User
import java.io.File
import java.io.IOError
import java.io.IOException
import java.nio.file.Files
import kotlin.random.Random

@Configuration
@EnableWebSecurity
open class SecurityConfig : WebSecurityConfigurerAdapter() {
    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http
                .csrf()
                .disable()
                .cors()
                .and()
                .authorizeRequests().anyRequest().authenticated()
                .and()
                .httpBasic()
    }

    @Autowired
    @Throws(java.lang.Exception::class)
    open fun configureGlobal(auth: AuthenticationManagerBuilder) {
        try {
            val authx = auth.inMemoryAuthentication()
            val f = File("/config/auth.conf")
            if (f.exists() && f.isFile && f.canRead()) {
                var dataLines = Files.readAllLines(File("/config/auth.conf").toPath())
                if (dataLines == null || dataLines.isEmpty()) {
                    throw IOException()
                }
                dataLines.forEach{
                    val data = it.trim().split(":")
                    if (data.size > 2) {
                        val user = User.withUsername(data[0]).password(data[1]).roles(data[2]).passwordEncoder { pass -> pass }.build()
                        authx.withUser(user)
                    }
                }
            } else {
                throw IOException()
            }
        } catch (e : Exception) {
            auth.inMemoryAuthentication()
                    .withUser("admin+" + System.nanoTime() + Random.nextLong())
                    .password("admin+" + System.nanoTime() + Random.nextLong())
                    .roles("USER")
        }
    }
}