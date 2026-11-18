package persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dominio.Alerta;
import dominio.NivelAlerta;
import dominio.TipoSensor;
import excecao.PersistenciaException;

/**
 * Grava e consulta os alertas emitidos. Como no MedicaoDAO, o SQL é fixo e os
 * valores entram por parâmetros (?) do PreparedStatement.
 */
public class AlertaDAO {

    private static final String SQL_INSERIR = """
            INSERT INTO alerta (codigo_sensor, tipo_sensor, nivel, estrategia,
                                valor_previsto, limite_estourado, id_ultima_medicao, data_hora)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_LISTAR_TODOS = """
            SELECT id, codigo_sensor, tipo_sensor, nivel, estrategia,
                   valor_previsto, limite_estourado, id_ultima_medicao, data_hora
              FROM alerta
             ORDER BY data_hora DESC, id DESC
            """;

    private static final String SQL_ULTIMO_DO_SENSOR_E_ESTRATEGIA = """
            SELECT id, codigo_sensor, tipo_sensor, nivel, estrategia,
                   valor_previsto, limite_estourado, id_ultima_medicao, data_hora
              FROM alerta
             WHERE codigo_sensor = ?
               AND estrategia = ?
             ORDER BY data_hora DESC, id DESC
             LIMIT 1
            """;

    private static final String SQL_CONTAR_TODOS = """
            SELECT COUNT(*)
              FROM alerta
            """;

    private final ConexaoBanco conexaoBanco;

    public AlertaDAO(ConexaoBanco conexaoBanco) {
        this.conexaoBanco = conexaoBanco;
    }

    /** Grava o alerta e o devolve com o id gerado pelo banco. */
    public Alerta salvar(Alerta alerta) {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            comando.setString(1, alerta.getCodigoSensor());
            comando.setString(2, alerta.getTipoSensor().name());
            comando.setString(3, alerta.getNivel().name());
            comando.setString(4, alerta.getEstrategia());
            comando.setDouble(5, alerta.getValorPrevisto());
            comando.setDouble(6, alerta.getLimiteEstourado());
            comando.setLong(7, alerta.getIdUltimaMedicao());
            comando.setTimestamp(8, Timestamp.valueOf(alerta.getDataHora()));
            comando.executeUpdate();

            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new PersistenciaException("o banco não devolveu o id do alerta gravado");
                }
                return alerta.comId(chaves.getLong(1));
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "não foi possível gravar o alerta do sensor " + alerta.getCodigoSensor(), e);
        }
    }

    /** Todos os alertas já emitidos, do mais recente para o mais antigo. */
    public List<Alerta> listarTodos() {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_LISTAR_TODOS)) {
            return lerAlertas(comando);
        } catch (SQLException e) {
            throw new PersistenciaException("não foi possível consultar os alertas emitidos", e);
        }
    }

    /** O alerta mais recente de um sensor para uma estratégia de previsão, se houver. */
    public Optional<Alerta> buscarUltimo(String codigoSensor, String estrategia) {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_ULTIMO_DO_SENSOR_E_ESTRATEGIA)) {
            comando.setString(1, codigoSensor);
            comando.setString(2, estrategia);
            List<Alerta> encontrados = lerAlertas(comando);
            if (encontrados.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(encontrados.get(0));
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "não foi possível consultar o último alerta do sensor " + codigoSensor, e);
        }
    }

    /** Quantos alertas já foram emitidos. */
    public int contarTodos() {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_CONTAR_TODOS);
             ResultSet resultado = comando.executeQuery()) {
            resultado.next();
            return resultado.getInt(1);
        } catch (SQLException e) {
            throw new PersistenciaException("não foi possível contar os alertas emitidos", e);
        }
    }

    private List<Alerta> lerAlertas(PreparedStatement comando) throws SQLException {
        List<Alerta> alertas = new ArrayList<>();
        try (ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) {
                alertas.add(new Alerta(
                        resultado.getLong("id"),
                        resultado.getString("codigo_sensor"),
                        TipoSensor.valueOf(resultado.getString("tipo_sensor")),
                        NivelAlerta.valueOf(resultado.getString("nivel")),
                        resultado.getString("estrategia"),
                        resultado.getDouble("valor_previsto"),
                        resultado.getDouble("limite_estourado"),
                        resultado.getLong("id_ultima_medicao"),
                        resultado.getTimestamp("data_hora").toLocalDateTime()));
            }
        }
        return alertas;
    }
}
