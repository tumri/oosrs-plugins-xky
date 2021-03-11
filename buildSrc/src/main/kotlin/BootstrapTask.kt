import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.get
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Paths
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


open class BootstrapTask : DefaultTask() {
    private data class ReleaseObject(val version: String,
                             val requires: String,
                             val date: String,
                             val url: String,
                             val sha512sum: String) {
        fun toJSON(): JSONObject {
            return JsonBuilder(
                    "version" to version,
                    "requires" to requires,
                    "date" to date,
                    "url" to url,
                    "sha512sum" to sha512sum
            ).jsonObject()
        }
    }
    private data class PluginObject(val projectUrl: String,
                            val provider: String,
                            val name: String,
                            val description: String,
                            val id: String,
                            val releases: ArrayList<ReleaseObject>) {
        fun toJSON(): JSONObject {
            return JsonBuilder(
                    "name" to name,
                    "id" to id,
                    "description" to description,
                    "provider" to provider,
                    "projectUrl" to projectUrl,
                    "releases" to releases.map { it.toJSON() }.toTypedArray()
            ).jsonObject()
        }
    }

    private fun formatDate(date: Date?) = with(date ?: Date()) {
        SimpleDateFormat("yyyy-MM-dd").format(this)
    }

    private fun hash(file: ByteArray): String {
        return MessageDigest.getInstance("SHA-512").digest(file).fold("", { str, it -> str + "%02x".format(it) }).toUpperCase()
    }

    private fun getBootstrap(): JSONArray? {
        val client = OkHttpClient()

        val url = "https://raw.githubusercontent.com/tumri/oosrs-plugins-xky/master/plugins.json"
        val request = Request.Builder()
                .url(url)
                .build()

        client.newCall(request).execute().use { response -> return JSONObject("{\"plugins\":${response.body!!.string()}}").getJSONArray("plugins") }
    }

    @TaskAction
    fun boostrap() {
        if (project == project.rootProject) {
            val bootstrapDir = File("${project.buildDir}/bootstrap")
            val bootstrapReleaseDir = File("${project.buildDir}/bootstrap/release")

            bootstrapDir.mkdirs()
            bootstrapReleaseDir.mkdirs()

            val plugins = ArrayList<JSONObject>()
            val baseBootstrap = getBootstrap()
                    ?.map { item ->
                        PluginObject(
                            projectUrl = (item as JSONObject).get("projectUrl") as String,
                            provider = (item as JSONObject).get("provider") as String,
                            name = (item as JSONObject).get("name") as String,
                            description = (item as JSONObject).get("description") as String,
                            id = (item as JSONObject).get("id") as String,
                            releases = ((item as JSONObject).get("releases") as JSONArray)
                                    .map { release ->
                                        ReleaseObject(
                                                date = (release as JSONObject).get("date") as String,
                                                sha512sum = (release as JSONObject).get("sha512sum") as String,
                                                version = (release as JSONObject).get("version") as String,
                                                url = (release as JSONObject).get("url") as String,
                                                requires = (release as JSONObject).get("requires") as String
                                        )
                                    } as ArrayList<ReleaseObject>
                        )
                    }
                    ?.associateBy({ it.id + it.releases.last().version + it.releases.last().sha512sum })

            project.subprojects.forEach {
                if (it.project.properties.containsKey("PluginName") && it.project.properties.containsKey("PluginDescription")) {
                    var pluginAdded = false

                    val plugin = it.project.tasks.get("jar").outputs.files.singleFile

                    val releaseHash = hash(plugin.readBytes())
                    val releaseVersion = it.project.version as String
                    val releaseRequires = ProjectVersions.apiVersion
                    val releaseDate = formatDate(Date())
                    val releaseUrl = "https://github.com/tumri/oosrs-plugins-xky/blob/master/release/${it.project.name}-${it.project.version}.jar?raw=true"

                    val pluginName = it.project.extra.get("PluginName") as String
                    val pluginId = nameToId(it.project.extra.get("PluginName") as String)
                    val pluginDescription = it.project.extra.get("PluginDescription") as String
                    val pluginProvider = "xKylee"
                    val pluginProjectUrl = "https://discord.gg/WUP22Dj7GT"

                    val pluginReleases = ArrayList<ReleaseObject>()
                    pluginReleases.add(ReleaseObject(
                            version = releaseVersion,
                            requires = releaseRequires,
                            date = baseBootstrap?.get(pluginId + releaseVersion + releaseHash)?.releases?.last()?.date ?: releaseDate,
                            url = releaseUrl,
                            sha512sum = releaseHash
                    ))
                    val pluginObject = PluginObject(
                            name = pluginName,
                            id = pluginId,
                            description = pluginDescription,
                            provider = pluginProvider,
                            projectUrl = pluginProjectUrl,
                            releases = pluginReleases
                    )

                    plugins.add(pluginObject.toJSON())

                    plugin.copyTo(Paths.get(bootstrapReleaseDir.toString(), "${it.project.name}-${it.project.version}.jar").toFile())
                }
            }

            File(bootstrapDir, "plugins.json").printWriter().use { out ->
                out.println(plugins.toString())
            }
        }
    }
}
