package dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import excecao.DadosInvalidosException;

class LimiaresTest {

    @Test
    void deveTratarComoNormalOValorAbaixoDoLimiteDeAtencao() {
        // Arrange
        Limiares limiares = new Limiares(10.0, 25.0, 50.0);

        // Act
        Optional<NivelAlerta> nivel = limiares.classificar(9.9);

        // Assert
        assertTrue(nivel.isEmpty(), "abaixo do limite de atenção não há alerta");
    }

    @Test
    void deveClassificarOValorNoNivelMaisGraveQueEleAtinge() {
        // Arrange
        Limiares limiares = new Limiares(10.0, 25.0, 50.0);

        // Act + Assert: o próprio limite já conta como atingido
        assertEquals(Optional.of(NivelAlerta.ATENCAO), limiares.classificar(10.0));
        assertEquals(Optional.of(NivelAlerta.ATENCAO), limiares.classificar(24.9));
        assertEquals(Optional.of(NivelAlerta.ALERTA), limiares.classificar(25.0));
        assertEquals(Optional.of(NivelAlerta.ALERTA), limiares.classificar(49.9));
        assertEquals(Optional.of(NivelAlerta.CRITICO), limiares.classificar(50.0));
        assertEquals(Optional.of(NivelAlerta.CRITICO), limiares.classificar(120.0));
    }

    @Test
    void deveInformarOLimiteDeCadaNivel() {
        // Arrange
        Limiares limiares = new Limiares(10.0, 25.0, 50.0);

        // Act + Assert
        assertEquals(10.0, limiares.getLimite(NivelAlerta.ATENCAO));
        assertEquals(25.0, limiares.getLimite(NivelAlerta.ALERTA));
        assertEquals(50.0, limiares.getLimite(NivelAlerta.CRITICO));
    }

    @Test
    void deveRecusarLimiaresForaDeOrdem() {
        // Arrange: o limite de alerta (10) está abaixo do de atenção (25)
        double atencao = 25.0;
        double alerta = 10.0;
        double critico = 50.0;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> new Limiares(atencao, alerta, critico));

        // Assert
        assertTrue(erro.getMessage().startsWith("limiar inválido"), erro.getMessage());
    }

    @Test
    void deveRecusarLimiaresRepetidos() {
        // Arrange + Act + Assert: dois níveis com o mesmo limite não se distinguem
        assertThrows(DadosInvalidosException.class, () -> new Limiares(10.0, 10.0, 50.0));
        assertThrows(DadosInvalidosException.class, () -> new Limiares(10.0, 50.0, 50.0));
    }

    @Test
    void deveRecusarLimiteDeAtencaoZeradoOuNegativo() {
        // Arrange + Act + Assert: com limite zero qualquer leitura viraria alerta
        assertThrows(DadosInvalidosException.class, () -> new Limiares(0.0, 25.0, 50.0));
        assertThrows(DadosInvalidosException.class, () -> new Limiares(-5.0, 25.0, 50.0));
    }

    @Test
    void deveRecusarLimiarQueNaoENumero() {
        // Arrange
        double semValor = Double.NaN;

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> new Limiares(10.0, semValor, 50.0));
    }
}
