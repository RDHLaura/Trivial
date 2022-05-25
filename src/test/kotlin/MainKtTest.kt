import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionScope
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.io.FileNotFoundException

internal class MainKtTest {
    @BeforeEach
    fun setUp() {
        conectarBD()
    }
    @Test
    fun conectarBD_test() {
        val connected = transaction { !connection.isClosed }
        assertTrue(connected)
    }
    @Test
    fun creacionTablas(){
        assertTrue(partidas.columns.size==3)
    }
    @Test
    fun volcarBD_test() {
        val fichero = File("src/main/resources/database.csv")
        transaction {
            val registros=partidas.selectAll().toList().size
            volcarBD()
            //se resta un registro al fichero porque el fichero cuenta la cabecera que son los titulos de las tablas
            assertEquals(fichero.readLines().size-1,registros)
            assertTrue(partidas.columns.size==3)
        }
    }

    @Test
    fun registrarPartida() {
        transaction {
            var registros=partidas.selectAll().toList()
            registrarPartida("Antonia", 5)
            var registros2=partidas.selectAll().toList()
            assertEquals(registros2.size, registros.size+1)
        }
    }

    @Test
    fun comprobarRespuesta() {
        val listaPreguntas=preguntas("preguntas.trivial")
        assertTrue(comprobarRespuesta(listaPreguntas[0]["Respuesta"]!!,listaPreguntas[0] ))
    }

    @Test
    fun preguntas() {
        val listaPreguntas=preguntas("preguntas.trivial")
        assertEquals(listaPreguntas.size, 10)
        assertEquals(listaPreguntas.distinct().size, 10)
        assertThrows( FileNotFoundException::class.java){preguntas("pregun.trivial")}
        assertThrows( IndexOutOfBoundsException::class.java){preguntas("2.trivial")}
    }


    @Test
    fun test_puntuaciones() {
        transaction {
            assertEquals(puntuaciones().toList().count(), partidas.selectAll().toList().count())
            assertEquals(puntuaciones("pepe").toList().size, partidas.select{partidas.usuario_p like "pepe"}.toList().size)

        }
    }

}