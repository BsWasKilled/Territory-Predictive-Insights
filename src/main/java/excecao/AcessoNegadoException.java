package excecao;

/**
 * Lançada quando alguém tenta alterar os limiares do território sem informar o
 * código de acesso correto. Nada é alterado quando ela acontece.
 */
public class AcessoNegadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
