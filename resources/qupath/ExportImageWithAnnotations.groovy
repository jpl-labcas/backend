// Script to export image with annotations
import qupath.imagej.tools.IJTools
import qupath.lib.gui.images.servers.RenderedImageServer
import qupath.lib.gui.viewer.overlays.HierarchyOverlay
import qupath.lib.regions.RegionRequest

import static qupath.lib.gui.scripting.QPEx.*

// Must downsample because image is too large to be exported
double downsample = 100

// Ccan change extension to export in different formats
String path = buildFilePath(PROJECT_BASE_DIR, 'export', getProjectEntry().getImageName() + '.png')

// Request the current viewer for settings, and current image (which may be used in batch processing)
def viewer = getCurrentViewer()
def imageData = getCurrentImageData()

// Create a rendered server that includes a hierarchy overlay using the current display settings
def server = new RenderedImageServer.Builder(imageData)
    .downsamples(downsample)
    .layers(new HierarchyOverlay(viewer.getImageRegionStore(), viewer.getOverlayOptions(), imageData))
    .build()

// Write the rendered image with annotations
mkdirs(new File(path).getParent())
writeImage(server, path)
print("Wrote image to path: "+path)
