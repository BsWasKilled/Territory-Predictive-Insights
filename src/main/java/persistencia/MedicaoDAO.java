package persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dominio.Medicao;
import dominio.TipoSensor;
import excecao.PersistenciaException;

/**
 * Grava e consulta medições no banco. Todo SQL é fixo e os valores entram por
 * parâmetros (?) do PreparedStatement, nunca por concatenação de texto.
 */
public class MedicaoDAO {

    private static final String SQL_INSERIR = """
            INSERT INTO medicao (codigo_sensor, tipo_sensor, valor, data_hora)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SQL_HISTORICO_POR_SENSOR = """
            SELECT id, codigo_sensor, tipo_sensor, valor, data_hora
              FROM medicao
             WHERE codigo_sensor = ?
             ORDER BY data_hora, id
            """;

    private static final String SQL_ULTIMAS_POR_SENSOR = """
            SELECT id, codigo_sensor, tipo_sensor, valor, data_hora
              FROM medicao
             WHERE codigo_sensor = ?
             ORDER BY data_hora DESC, id DESC
             LIMIT ?
            """;

    private static final String SQL_CONTAR_POR_SENSOR = """
            SELECT COUNT(*)
              FROM medicao
             WHERE codigo_sensor = ?
            """;

    private final ConexaoBanco conexaoBanco;

    public MedicaoDAO(ConexaoBanco conexaoBanco) {
        this.conexaoBanco = conexaoBanco;
    }

    /** Grava a medição e a devolve com o id gerado pelo banco. */
    public Medicao salvar(Medicao medicao) {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            comando.setString(1, medicao.getCodigoSensor());
            comando.setString(2, medicao.getTipo().name());
            comando.setDouble(3, medicao.getValor());
            comando.setTimestamp(4, Timestamp.valueOf(medicao.getDataHora()));
            comando.executeUpdate();

            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new PersistenciaException("o banco não devolveu o id da medição gravada");
                }
                return medicao.comId(chaves.getLong(1));
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "não foi possível gravar a medição do sensor " + medicao.getCodigoSensor(), e);
        }
    }

    /** Todo o histórico de um sensor, em ordem cronológica (a leitura mais antiga primeiro). */
    public List<Medicao> buscarHistoricoPorSensor(String codigoSensor) {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_HISTORICO_POR_SENSOR)) {
            comando.setString(1, codigoSensor);
            return lerMedicoes(comando);
        } catch (SQLException e) {
            throw new PersistenciaException("não foi possível consultar o histórico do sensor " + codigoSensor, e);
        }
    }

    /**
     * As leituras mais recentes de um sensor, no máximo 'quantidade', devolvidas
     * em ordem cronológica (a mais antiga primeiro), que é como as previsões esperam.
     */
    public List<Medicao> buscarUltimasPorSensor(String codigoSensor, int quantidade) {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_ULTIMAS_POR_SENSOR)) {
            comando.setString(1, codigoSensor);
            comando.setInt(2, quantidade);
            List<Medicao> ultimas = lerMedicoes(comando);
            Collections.reverse(ultimas); // o banco devolve da mais nova para a mais antiga
            return ultimas;
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "não foi possível consultar as últimas leituras do sensor " + codigoSensor, e);
        }
    }

    /** Quantas leituras do sensor já estão gravadas. */
    public int contarPorSensor(String codigoSensor) {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_CONTAR_POR_SENSOR)) {
            comando.setString(1, codigoSensor);
            try (ResultSet resultado = comando.executeQuery()) {
                resultado.next();
                return resultado.getInt(1);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("não foi possível contar as leituras do sensor " + codigoSensor, e);
        }
    }

    private List<Medicao> lerMedicoes(PreparedStatement comando) throws SQLException {
        List<Medicao> medicoes = new ArrayList<>();
        try (ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) {
                medicoes.add(new Medicao(
                        resultado.getLong("id"),
                        resultado.getString("codigo_sensor"),
                        TipoSensor.valueOf(resultado.getString("tipo_sensor")),
                        resultado.getDouble("valor"),
                        resultado.getTimestamp("data_hora").toLocalDateTime()));
            }
        }
        return medicoes;
    }
}
