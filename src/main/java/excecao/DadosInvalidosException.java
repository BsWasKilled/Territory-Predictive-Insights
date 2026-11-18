package excecao;

/**
 * Lançada quando um dado não respeita as regras do sistema: medição fora da
 * faixa do sensor, população negativa, limiar inválido ou histórico que não
 * sustenta uma previsão.
 *
 * É não verificada (RuntimeException) porque representa um valor que não
 * deveria ter chegado até ali: quem valida lança, e o menu captura e explica.
 */
public class DadosInvalidosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
