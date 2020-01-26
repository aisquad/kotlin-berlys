import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Costumer (val id: Int, val name: String, val town: String, val PVL: Float)

data class Route (val id: Int?, val name: String?, val costumers: MutableList<Costumer>)

class SourceFile(){
    val sep = File.separator
    var sourceFilename: String = "Volumen Rutas.txt"
    var readDir: String = listOf("C:", "Download").joinToString(sep)
    private val writeDirArray = listOf(
        "C:", "Users", "igorr", "OneDrive", "Eclipse", "Python", "dades", "Berlys"
    )
    var writeDir: String = writeDirArray.joinToString (sep)
    var date = LocalDate.now()
    var file: File = File("")

    fun checkWriteDir() {
        // Create new folder when it doesn't exist.
        writeDir = listOf(writeDir, date.year, "%02d".format(date.monthValue)).joinToString(sep)
        if (!File(writeDir).exists()) {
            File(writeDir).mkdir()
        }
    }
    fun checkFile() {
        /*
            Read downloaded file from Download folder, if it isn't there then read it from data folder.
            If file is in Download folder, move it to data folder.
         */
        file = File(readDir, sourceFilename)

        if (!file.exists()) {
            checkWriteDir()
            val files = File(writeDir).listFiles()
            val filename = files[files.lastIndex]
            file = File(filename.toURI())
        } else {
            val filename = listOf(
                writeDir, date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            ).joinToString(sep) + ".txt"
            file.renameTo(File(filename))
        }
    }

    fun readSourceFile() : String {
        checkFile()
        var data: String = file.readText()
        return data
    }

    fun fetchData(allowedRoutes: List<Int>) : MutableList<Route> {
        val routes : MutableList<Route> = mutableListOf()
        val routePattern = Regex(
            "25\\s+BERLYS ALIMENTACION S\\.A\\.U\\s+[\\d:]+\\s+[\\d.]+\\s+Volumen de pedidos de la ruta :\\s+" +
                "(?<routeid>\\d+)\\s+(?<routedesc>[^\\n]+)\\s+Día de entrega :\\s+(?<date>[^ ]{10})(?<stops>.+?)" +
                "NUMERO DE CLIENTES\\s+:\\s+(?<costnum>\\d+).+?" +
                "SUMA VOLUMEN POR RUTA\\s+:\\s+(?<volamt>[\\d,.]+) (?<um1>(?:PVL|KG)).+?" +
                "SUMA KG POR RUTA\\s+:\\s+(?<weightamt>[\\d,.]+) (?<um2>(?:PVL|KG)).+?" +
                "(?:CAPACIDAD TOTAL CAMIÓN\\s+:\\s+(?<truckcap>[\\d,.]+) (?<um3>(?:PVL|KG)))?", RegexOption.DOT_MATCHES_ALL
            )
        val costumersPattern = Regex(
            "(?<code>\\d{10}) (?<costumer>.{35}) (?<town>.{20}) (?<ordnum>.{10}) (?<vol>.{11})(?: (?<UM>.{2,3}))?"
        )
        val text = readSourceFile()

        //Routes iteration
        routePattern.findAll(text).forEach { matchResult ->
            val routeid: Int = matchResult.groups["routeid"]!!.value.toInt()
            if (routeid in allowedRoutes) {
                val items = costumersPattern.findAll(matchResult.groups["stops"]!!.value)
                var costumers = mutableListOf<Costumer>()

                //Costumers iteration
                items.forEach {
                    var vol = it.groups["vol"]!!.value
                    vol = vol.replace(".", "")
                    vol = vol.replace(',', '.')
                    val costumer = Costumer(
                        it.groups["code"]!!.value.toInt(),
                        it.groups["costumer"]!!.value.trim(' '),
                        it.groups["town"]!!.value.trim(' '),
                        vol.toFloat()
                    )
                    costumers.add(costumer)
                }
                val route = Route(
                    matchResult.groups["routeid"]!!.value.toInt(),
                    matchResult.groups["routedesc"]!!.value.trimStart('2', '5', ' '),
                    costumers
                )
                routes.add(route)
            }
        }
        return routes
    }
}

class Berlys {
    val source = SourceFile()
    val knownRoutes : List<Int> = listOf(678, 679, 680, 681, 682, 686, 688, 696)
    var routes : MutableList<Route> = mutableListOf()

    fun showRoutes () {
        var i = 1
        routes.forEach { route ->
            println("${route.id} ${route.name}")
            route.costumers.forEach { costumer ->
                println("${i++}\t${costumer.name}\t${costumer.town}\t${costumer.PVL} PVL")
            }
            println()
        }
    }
    fun showAssignedRoutes(assignedRoutes: List<Int>) {
        var i = 0
        assignedRoutes.forEach { assignedRoute ->
            routes.forEach { route ->
                if (assignedRoute == route.id) {
                    println("${route.id}\t${route.name}")
                    route.costumers.forEach { costumer ->
                        println("${++i}\t${costumer.name}\t${costumer.town}\t${costumer.PVL} PVL")
                    }
                    println()
                }
            }
        }
    }
    fun run (){
        routes = source.fetchData(knownRoutes)
        val assignedRoutes = listOf(680, 688)
        showAssignedRoutes(assignedRoutes)
     }
}

fun main() {
    val berlys = Berlys()
    berlys.run()
}