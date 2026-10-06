package sv.clinica.api.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TramoTest {

    @Test
    void intersectaConElHorarioDeLaClinica() {
        // turno 8–13 y 15–19, clínica 9–17 → 9–13 y 15–17
        assertThat(Tramo.intersectar(List.of(new Tramo(480, 780), new Tramo(900, 1140)), new Tramo(540, 1020)))
                .containsExactly(new Tramo(540, 780), new Tramo(900, 1020));
    }

    @Test
    void restaAlmuerzoYCitas() {
        // 8–17 menos almuerzo 12–13 y cita 10–11 → 8–10, 11–12, 13–17
        assertThat(Tramo.restar(List.of(new Tramo(480, 1020)), List.of(new Tramo(720, 780), new Tramo(600, 660))))
                .containsExactly(new Tramo(480, 600), new Tramo(660, 720), new Tramo(780, 1020));
    }

    @Test
    void unirFusionaTramosQueSeTocan() {
        assertThat(Tramo.unir(List.of(new Tramo(600, 660), new Tramo(540, 600), new Tramo(700, 720))))
                .containsExactly(new Tramo(540, 660), new Tramo(700, 720));
    }
}
