package persistencia;

import java.util.UUID;

/**
 * Apoio aos testes dos DAOs: cria um banco H2 só em memória, com um nome
 * diferente a cada chamada. Nada é gravado em disco e um teste nunca enxerga
 * os dados de outro.
 */
final class BancoDeTeste {

    private BancoDeTeste() {
        // só tem métodos estáticos: não deve ser instanciada
    }

    /** Banco em memória novo, já com as tabelas criadas. */
    static ConexaoBanco novo() {
        String url = "jdbc:h2:mem:teste_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";
        ConexaoBanco conexaoBanco = new ConexaoBanco(url, "sa", "");
        conexaoBanco.prepararBanco();
        return conexaoBanco;
    }
}
