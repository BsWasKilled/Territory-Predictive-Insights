package dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;

class SensorSimuladoTest {

    private static final double TOLERANCIA = 0.0001;

    /** Sorteio que faz o sensor responder: fica acima da chance de falha. */
    private static final double RESPONDE = 0.50;
    /** Sorteio que faz o sensor falhar: fica abaixo da chance de falha. */
    private static final double NAO_RESPONDE = 0.01;
    /** Sorteio que deixa o equipamento sem defeito. */
    private static final double SEM_DEFEITO = 0.50;
    /** Sorteio que faz o equipamento devolver um valor impossível. */
    private static final double COM_DEFEITO = 0.01;
    /** Sorteio que resulta em oscilação zero na leitura. */
    private static final double SEM_OSCILACAO = 0.50;

    /**
     * Gerador de teste: em vez de sortear, devolve sempre os valores
     * combinados, na ordem, recomeçando do primeiro quando chega ao fim.
     * A cada leitura o sensor pede três sorteios: se responde, se está com
     * defeito e quanto a leitura oscila.
     */
    private static final class SorteioCombinado extends Random {

        private static final long serialVersionUID = 1L;

        private final double[] valores;
        private int proximo;

        SorteioCombinado(double... valores) {
            this.valores = valores;
        }

        @Override
        public double nextDouble() {
            double valor = valores[proximo];
            proximo = (proximo + 1) % valores.length;
            return valor;
        }
    }

    private static Random sorteioDeLeituraNormal() {
        return new SorteioCombinado(RESPONDE, SEM_DEFEITO, SEM_OSCILACAO);
    }

    @Test
    void deveLerChuvaFracaNoComecoDaTempestade() throws FonteIndisponivelException {
        // Arrange
        Sensor pluviometro = new SensorChuva("PLUV-01", sorteioDeLeituraNormal());

        // Act
        double primeiraLeitura = pluviometro.lerValor();

        // Assert: 2 mm/h de garoa + 58 * sen²(pi/16) = 4,2 mm/h
        assertEquals(4.2, primeiraLeitura, TOLERANCIA);
        assertTrue(pluviometro.verificarFuncionamento());
    }

    @Test
    void deveChegarAoPicoDaChuvaNaOitavaLeitura() throws FonteIndisponivelException {
        // Arrange
        Sensor pluviometro = new SensorChuva("PLUV-01", sorteioDeLeituraNormal());

        // Act
        double leitura = 0.0;
        for (int i = 1; i <= 8; i++) {
            leitura = pluviometro.lerValor();
        }

        // Assert: no pico da tempestade a chuva simulada é de 60 mm/h
        assertEquals(60.0, leitura, TOLERANCIA);
    }

    @Test
    void deveFazerOCorregoSubirDepoisDaChuva() throws FonteIndisponivelException {
        // Arrange
        Sensor regua = new SensorNivelRio("NIVEL-01", sorteioDeLeituraNormal());

        // Act: o córrego reage com 3 leituras de atraso, então o pico vem na 11ª
        double naOitavaLeitura = 0.0;
        double naDecimaPrimeiraLeitura = 0.0;
        for (int i = 1; i <= 11; i++) {
            double leitura = regua.lerValor();
            if (i == 8) {
                naOitavaLeitura = leitura;
            }
            if (i == 11) {
                naDecimaPrimeiraLeitura = leitura;
            }
        }

        // Assert
        assertEquals(340.0, naDecimaPrimeiraLeitura, TOLERANCIA);
        assertTrue(naOitavaLeitura < naDecimaPrimeiraLeitura, "na 8ª leitura o córrego ainda está subindo");
    }

    @Test
    void deveLancarFonteIndisponivelQuandoOSensorNaoResponde() {
        // Arrange
        Sensor pluviometro = new SensorChuva("PLUV-01", new SorteioCombinado(NAO_RESPONDE));

        // Act
        FonteIndisponivelException erro = assertThrows(FonteIndisponivelException.class, pluviometro::lerValor);

        // Assert
        assertTrue(erro.getMessage().contains("PLUV-01"), erro.getMessage());
        assertFalse(pluviometro.verificarFuncionamento());
    }

    @Test
    void deveVoltarAFuncionarQuandoOSensorRespondeDeNovo() throws FonteIndisponivelException {
        // Arrange: falha na primeira tentativa e responde normalmente na segunda
        Sensor pluviometro = new SensorChuva("PLUV-01",
                new SorteioCombinado(NAO_RESPONDE, RESPONDE, SEM_DEFEITO, SEM_OSCILACAO));
        assertThrows(FonteIndisponivelException.class, pluviometro::lerValor);

        // Act
        double leitura = pluviometro.lerValor();

        // Assert: a tentativa que falhou não conta como leitura da tempestade
        assertEquals(4.2, leitura, TOLERANCIA);
        assertTrue(pluviometro.verificarFuncionamento());
    }

    @Test
    void deveDevolverValorImpossivelQuandoOEquipamentoEstaComDefeito() throws FonteIndisponivelException {
        // Arrange
        Sensor pluviometro = new SensorChuva("PLUV-01", new SorteioCombinado(RESPONDE, COM_DEFEITO));

        // Act
        double leituraComDefeito = pluviometro.lerValor();

        // Assert: o sensor entrega o valor, e é a validação da Medicao que o barra
        assertEquals(-1.0, leituraComDefeito, TOLERANCIA);
        assertThrows(DadosInvalidosException.class,
                () -> new Medicao("PLUV-01", TipoSensor.CHUVA, leituraComDefeito, java.time.LocalDateTime.now()));
    }

    @Test
    void deveManterAsLeiturasNormaisDentroDaFaixaDeCadaSensor() throws FonteIndisponivelException {
        // Arrange: oscilação máxima para cima, o pior caso para estourar a faixa
        double oscilacaoMaxima = 0.999;
        Sensor[] sensores = {
            new SensorChuva("PLUV-01", new SorteioCombinado(RESPONDE, SEM_DEFEITO, oscilacaoMaxima)),
            new SensorNivelRio("NIVEL-01", new SorteioCombinado(RESPONDE, SEM_DEFEITO, oscilacaoMaxima)),
            new SensorDrenagem("GAL-01", new SorteioCombinado(RESPONDE, SEM_DEFEITO, oscilacaoMaxima))
        };

        for (Sensor sensor : sensores) {
            // Act: duas tempestades completas (32 leituras)
            for (int i = 1; i <= 32; i++) {
                double leitura = sensor.lerValor();

                // Assert
                assertTrue(leitura >= sensor.getTipo().getMinimo() && leitura <= sensor.getTipo().getMaximo(),
                        sensor.getCodigo() + " leu " + leitura + " na leitura " + i);
            }
        }
    }

    @Test
    void deveInformarOTipoDeCadaSensor() {
        // Arrange
        Sensor pluviometro = new SensorChuva("PLUV-01");
        Sensor regua = new SensorNivelRio("NIVEL-01");
        Sensor galeria = new SensorDrenagem("GAL-01");

        // Act + Assert
        assertEquals(TipoSensor.CHUVA, pluviometro.getTipo());
        assertEquals(TipoSensor.NIVEL_RIO, regua.getTipo());
        assertEquals(TipoSensor.DRENAGEM, galeria.getTipo());
        assertEquals("GAL-01", galeria.getCodigo());
    }

    @Test
    void deveRecusarSensorSemCodigo() {
        // Arrange
        String codigoEmBranco = "";

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> new SensorChuva(codigoEmBranco));
    }
}
