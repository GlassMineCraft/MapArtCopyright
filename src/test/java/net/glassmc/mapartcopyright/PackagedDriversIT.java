package net.glassmc.mapartcopyright;

import org.junit.jupiter.api.Test;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Driver;
import java.util.Properties;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

/** Verify the actual shaded artifact, isolated from Maven's unshaded JDBC dependencies. */
class PackagedDriversIT {
    private URLClassLoader packagedClasses() throws Exception {
        URL plugin = Path.of(System.getProperty("plugin.jar")).toUri().toURL();
        // SLF4J is provided by Paper at runtime, not bundled into the plugin.
        URL slf4j = org.slf4j.Logger.class.getProtectionDomain().getCodeSource().getLocation();
        return new URLClassLoader(new URL[]{plugin, slf4j}, ClassLoader.getPlatformClassLoader());
    }

    private void roundTrip(String driverName, String url) throws Exception {
        try (var loader = packagedClasses()) {
            Driver driver = (Driver) loader.loadClass(driverName).getConstructor().newInstance();
            try (var connection = driver.connect(url, new Properties()); var statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE packaged_check (title VARCHAR(100))");
                statement.executeUpdate("INSERT INTO packaged_check VALUES ('Gallery')");
                try (var result = statement.executeQuery("SELECT title FROM packaged_check")) {
                    assertTrue(result.next());
                    assertEquals("Gallery", result.getString(1));
                }
            }
        }
    }

    @Test void relocatedH2DriverCanWriteAndReadFromPackagedJar() throws Exception {
        roundTrip("net.glassmc.shaded.h2.Driver", "jdbc:h2:mem:packaged");
    }

    @Test void sqliteDriverAndNativeLibraryWorkFromPackagedJar() throws Exception {
        roundTrip("org.sqlite.JDBC", "jdbc:sqlite::memory:");
    }

    @Test void mysqlDriverIsLoadableAndServiceEntriesAreRelocated() throws Exception {
        try (var loader = packagedClasses()) {
            Driver driver = (Driver) loader.loadClass("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
            assertTrue(driver.acceptsURL("jdbc:mysql://localhost/mapart"));
        }
        try (var jar = new JarFile(System.getProperty("plugin.jar"))) {
            var service = jar.getJarEntry("META-INF/services/java.sql.Driver");
            assertNotNull(service);
            String drivers = new String(jar.getInputStream(service).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(drivers.contains("net.glassmc.shaded.h2.Driver"));
            assertTrue(drivers.contains("org.sqlite.JDBC"));
            assertTrue(drivers.contains("com.mysql.cj.jdbc.Driver"));
            assertNull(jar.getJarEntry("org/bukkit/Bukkit.class"));
        }
    }
}
