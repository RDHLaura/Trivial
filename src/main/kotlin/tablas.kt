import org.jetbrains.exposed.sql.*

object partidas :Table(){
    val usuario_p=text("jugador")
    val puntuacion_p =integer("puntuación")
    val fecha_p=text("fecha")
}
