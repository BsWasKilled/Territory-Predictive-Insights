package excecao;

/**
 * Lançada pelos DAOs quando o banco de dados falha (arquivo em uso, disco sem
 * espaço, SQL recusado). Guarda a SQLException original como causa, para que o
 * erro técnico não se perca, e evita que o restante do sistema dependa de JDBC.
 */
public class PersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensagem) {
        super(mensagem);
    }

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
