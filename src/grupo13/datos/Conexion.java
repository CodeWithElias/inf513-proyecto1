package grupo13.datos;

import grupo13.config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Abre conexiones JDBC a PostgreSQL (db_grupo13sa). */
public final class Conexion {

    private Conexion() {
    }

    public static Connection abrir() throws SQLException {
        String url = "jdbc:postgresql://" + Config.get("db.host") + ":" + Config.get("db.puerto")
                + "/" + Config.get("db.nombre");
        return DriverManager.getConnection(url, Config.get("db.usuario"), Config.get("db.password"));
    }

    // ===== Ayudantes genéricos (usados desde CU3). Los parámetros se pasan siempre por PreparedStatement. =====

    /** Trabajo que se ejecuta dentro de una transacción. */
    @FunctionalInterface
    public interface Transaccion<T> {
        T ejecutar(Connection c) throws SQLException;
    }

    /** Ejecuta 'trabajo' en una transacción: commit si todo sale bien, rollback si falla. */
    public static <T> T enTransaccion(Transaccion<T> trabajo) throws SQLException {
        try (Connection c = abrir()) {
            c.setAutoCommit(false);
            try {
                T resultado = trabajo.ejecutar(c);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** SELECT con parámetros; devuelve las filas como texto. */
    public static List<String[]> consultar(String sql, Object... params) throws SQLException {
        try (Connection c = abrir()) {
            return consultar(c, sql, params);
        }
    }

    public static List<String[]> consultar(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = preparar(c, sql, params); ResultSet rs = ps.executeQuery()) {
            return leerFilas(rs);
        }
    }

    /** Primera fila del SELECT o null si no hay resultados. */
    public static String[] consultarUno(String sql, Object... params) throws SQLException {
        List<String[]> filas = consultar(sql, params);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /** Primera columna de la primera fila (o null). */
    public static String valor(Connection c, String sql, Object... params) throws SQLException {
        List<String[]> filas = consultar(c, sql, params);
        return filas.isEmpty() ? null : filas.get(0)[0];
    }

    public static String valor(String sql, Object... params) throws SQLException {
        try (Connection c = abrir()) {
            return valor(c, sql, params);
        }
    }

    /** INSERT/UPDATE/DELETE con parámetros; devuelve las filas afectadas. */
    public static int ejecutar(String sql, Object... params) throws SQLException {
        try (Connection c = abrir()) {
            return ejecutar(c, sql, params);
        }
    }

    public static int ejecutar(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = preparar(c, sql, params)) {
            return ps.executeUpdate();
        }
    }

    /** INSERT que devuelve el ID generado (la sentencia debe terminar en "RETURNING id_xxx"). */
    public static int insertar(Connection c, String sql, Object... params) throws SQLException {
        return Integer.parseInt(valor(c, sql, params));
    }

    public static int insertar(String sql, Object... params) throws SQLException {
        try (Connection c = abrir()) {
            return insertar(c, sql, params);
        }
    }

    /**
     * null = no existe, true/false = valor de la columna activo.
     * 'tabla' y 'columnaId' son constantes del código, nunca datos del usuario.
     */
    public static Boolean estaActivo(String tabla, String columnaId, int id) throws SQLException {
        String v = valor("SELECT activo FROM " + tabla + " WHERE " + columnaId + " = ?", id);
        return v == null ? null : v.equals("t");
    }

    public static int contar(String sql, Object... params) throws SQLException {
        return Integer.parseInt(valor(sql, params));
    }

    /** Los null se envían como NULL; LocalDate, BigDecimal, Integer y String se pasan con setObject. */
    private static PreparedStatement preparar(Connection c, String sql, Object... params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                ps.setNull(i + 1, Types.NULL);
            } else {
                ps.setObject(i + 1, params[i]);
            }
        }
        return ps;
    }

    /** Convierte todas las filas de un ResultSet en arreglos de texto. */
    public static List<String[]> leerFilas(ResultSet rs) throws SQLException {
        int n = rs.getMetaData().getColumnCount();
        List<String[]> filas = new ArrayList<>();
        while (rs.next()) {
            String[] f = new String[n];
            for (int i = 0; i < n; i++) {
                f[i] = rs.getString(i + 1);
            }
            filas.add(f);
        }
        return filas;
    }
}
