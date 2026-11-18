package excecao;

/**
 * Lançada quando a fonte de uma medição falha: o sensor (ou o gerador de dados
 * que o simula) não respondeu.
 *
 * É verificada (extends Exception) de propósito: sensor fora do ar é uma
 * situação esperada em campo, então o compilador obriga quem pede uma leitura
 * a decidir o que fazer quando ela não vem.
 */
public class FonteIndisponivelException extends Exception {

    private static final long serialVersionUID = 1L;

    public FonteIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
