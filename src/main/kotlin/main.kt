import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.lang.IndexOutOfBoundsException
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    conectarBD()
    val usuario= datosUsuario()
    val archivo="preguntas.trivial"
    while (true) {
        menuPrograma(archivo, usuario)
    }

}

fun menuPrograma(archivo:String, usuario: String){
    impCabezera()
    val menu=
        "1. Empezar partida\n" +
        "2. Ver todos los resultados obtenidos.\n" +
        "3. Ver el ranking de puntuaciones globales.\n" +
        "4. Salir\n"
    println(menu)
    var eleccion=readLine()
    while (eleccion !in listOf<String>("1","2","3","4")){
        println("Escriba una opción válida")
        eleccion= readLine()
    }
    when(eleccion){
        "1"->{
            try{
                val preguntas = preguntas(archivo)
                val puntuacion=realizarPreguntas(preguntas)
                registrarPartida(usuario, puntuacion)
                //se almacena la bd en un archivo csv
                volcarBD()
                imprimirResultadoPartida(usuario, puntuacion)
            } catch (e: FileNotFoundException) {
                println("Archivo '$archivo' no encontrado.")
            }catch (e:IndexOutOfBoundsException){
                println("Preguntas insuficientes en el archivo $archivo")
            }

        }
        "2"->{
            //imprime los resultados globales del jugador
            println("\n\nPuntuaciones anteriores de $usuario:")
            imprimirBD(puntuaciones(usuario))
        }
        "3"->{
            //imprime los resultados globales de todos los jugadores
            println("\n\nPuntuaciones globales:")
            imprimirBD(puntuaciones())
        }
        "4"-> exitProcess(0)

    }

}
fun conectarBD(){
    Database.connect("jdbc:sqlite:src/main/resources/database.db")
    transaction {
        //se crea la tabla partidas en caso de que no exista
        SchemaUtils.create(partidas)
    }
}
//imprime la cabecera del juego
fun impCabezera(){
    println("\n\n\n**************************************************************")
    println("************************ TRIVIAL *****************************")
    println("**************************************************************")

}
//Pedir datos al usuario
fun datosUsuario():String{
    var user:String?
    do{
        println("Nombre de usuario: ")
        user= readLine()
    }while(user==null )
    return user
}
/*recibe una lista con las preguntas, recorre la lista haciendo la llamada a la función pregunta que imprime la pregunta y pide respuesta al usuario
 llama a comprobar repuesta que devuelve un boolean que indica si la respuesta es correcta*/
fun realizarPreguntas(preguntas:List<Map<String,String>>):Int{
    var puntuacion=0
    for (pregunta in preguntas){
        if(pregunta(pregunta)){
            puntuacion+=1
        }
    }
    return puntuacion
}
/*Almacena las preguntas del archivo preguntas.trivial en una lista de diccionario baraja la lista, y devuelve las 10 primeras preguntas*/
fun preguntas(archivo:String):List<Map<String,String>> {
    var listaPreguntas = mutableListOf<Map<String, String>>()
    lateinit var fichero: List<String>
    fichero = File("src/main/resources/$archivo").readLines()
    var indice = 0
    for (index in 6..fichero.size step 6) {
        //mediante una sublist se selecciona una pregunta con su respuesta y opciones
        val pregunta = fichero.subList(indice, index)
        //la pregunta se convierte en diccionario y se almacena en listaPreguntas
        var diccionario = mutableMapOf<String, String>(
            "Pregunta" to "",
            "Respuesta" to "",
            "A" to "",
            "B" to "",
            "C" to "",
            "D" to ""
        )
        pregunta.forEach {
            when (pregunta.indexOf(it)) {
                0 -> diccionario["Pregunta"] = it
                1 -> diccionario["Respuesta"] = it
                2 -> diccionario["A"] = it
                3 -> diccionario["B"] = it
                4 -> diccionario["C"] = it
                5 -> diccionario["D"] = it
            }
        }
        listaPreguntas.add(diccionario)
        indice = index
    }
    if (listaPreguntas.size<10){
        throw IndexOutOfBoundsException()
    }
    return listaPreguntas.shuffled().subList(0, 10)
}
//imprime la pregunta que se le pasa por parámetro y comprueba que la respuesta sea correcta.
fun pregunta(pregunta:Map<String,String>):Boolean{
    //imprime la pregunta con las opciones
    pregunta.forEach {
        if (it.key != "Respuesta") {
            println(it.key + ": " + it.value)
        }
    }
    //el usuario introduce su respuesta si no es una de las posibles opciones o es un null volverá a pedir una respuesta válida
    var respuesta:String?=readLine()
    while(respuesta==null || respuesta.uppercase() !in listOf<String>("A", "B", "C", "D")){
        println("La respuesta no es válida debe introducir (\"A\", \"B\", \"C\", \"D\")")
        respuesta= readLine()
    }
    return comprobarRespuesta(respuesta,pregunta)

}
fun comprobarRespuesta(respuesta:String, pregunta: Map<String, String>):Boolean{
    //comprueba que la respuesta sea correcta, en caso contrario le indica cual era
    if (respuesta.uppercase()== pregunta["Respuesta"]){
        println("¡Respuesta correcta!")
        return true
    }else{
        println("Has fallado. La respuesta correcta es: ${pregunta[pregunta["Respuesta"]]}")
        return false
    }
}
//registra la partida en la base de datos
fun registrarPartida(nombre: String, puntuacion:Int){
    val fecha=SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
    transaction {
        partidas.insert {
            it[usuario_p] = nombre
            it[puntuacion_p] = puntuacion
            it[fecha_p]=fecha
        }
    }
}
//imprime el resultado de la partida actual
fun imprimirResultadoPartida(usuario: String, puntuacion: Int){
    println("**********************************")
    println("Puntuación obtenida: "+ puntuacion)
    println("**********************************")

}
//devuelve las puntuaciones de un usuario o de toda la base de datos
fun puntuaciones(usuario: String?=null):Query{
    lateinit var partidasUsuario:Query
    transaction {
        if (usuario==null)
            partidasUsuario= partidas.selectAll().orderBy(partidas.puntuacion_p to SortOrder.DESC)
        else
            partidasUsuario= partidas.select{partidas.usuario_p like usuario}.orderBy(partidas.puntuacion_p to SortOrder.DESC)
    }
    return partidasUsuario
}
//imprime todas las puntuaciones de todos los usuarios ordenados de forma descendente
fun imprimirBD(listaPartidas:Query){
    //imprimir BD
    val leftAlignFormat = "| %-15s | %-15s | %-15s |%n"
    System.out.format("+-----------------+-----------------+---------------------+%n");
    System.out.format("| Usuario         | Puntuación      | Fecha               |%n");
    System.out.format("+-----------------+-----------------+---------------------+%n");
    transaction {
        listaPartidas.forEach { System.out.format(leftAlignFormat, it[partidas.usuario_p], it[partidas.puntuacion_p] ,it[partidas.fecha_p]) }
    }
    System.out.format("+-----------------+-----------------+---------------------+%n");
}
//vuelca los datos de la base de datos en un archivo csv
fun volcarBD(){
    val fichero =File("src/main/resources/database.csv")
    fichero.createNewFile()
    val claves = listOf<String>("Nombre", "Puntuación", "Fecha")
    fichero.writeText("Nombre, Puntuación, Fecha\n")
    transaction {
        partidas.selectAll().forEach {fichero.appendText( it[partidas.usuario_p]+", "+ it[partidas.puntuacion_p] +", "+it[partidas.fecha_p]+"\n") }
    }
}














