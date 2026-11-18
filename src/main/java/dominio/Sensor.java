package dominio;

import excecao.FonteIndisponivelException;

/**
 * Contrato de qualquer sensor do território. O restante do sistema só conhece
 * esta interface: tanto faz se a leitura vem de um sensor simulado ou, no
 * futuro, de um equipamento ou de uma fonte pública de dados.
 */
public interface Sensor {

    /** Identificador único do sensor; é por ele que o histórico é consultado. */
    String getCodigo();

    /** Grandeza medida, que define a unidade e a faixa válida das leituras. */
    TipoSensor getTipo();

    /**
     * Faz uma leitura.
     *
     * @throws FonteIndisponivelException se o sensor não responder
     */
    double lerValor() throws FonteIndisponivelException;

    /** Informa se o sensor respondeu na última tentativa de leitura. */
    boolean verificarFuncionamento();
}
