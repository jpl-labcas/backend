// Script to export the image annotations in GeoJson format
def imageData = getCurrentImageData()

// Exract the annotations
// def annotations = getAnnotationObjects()
// def annotations = getDetectionObjects()
def defaultColor = getColorRGB(200, 0, 0)
def annotations = getAnnotationObjects().each {
   if (it.getColorRGB() == null) {
     def newColor = it.getPathClass() == null ? defaultColor : it.getPathClass().getColor()
     it.setColorRGB(newColor)
  }
}
fireHierarchyUpdate()
println annotations
boolean prettyPrint = true
def gson = GsonTools.getInstance(prettyPrint)
println gson.toJson(annotations)

// Define output path (relative to project)
//def outputDir = buildFilePath(PROJECT_BASE_DIR, 'geoJson')
def outputDir = "/home/cinquini/geoJson"
mkdirs(outputDir)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
def path = buildFilePath(outputDir, name + "-geo.json")
println "Writing to path=" + path

// Write annotations to file
try {
    file = new FileWriter(path);
    file.write(gson.toJson(annotations)); 
} catch (IOException e) {
    e.printStackTrace();
} finally {
    try {
        file.flush();
        file.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

