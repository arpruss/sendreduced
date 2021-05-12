R=SendReduced/src/$1/res
convert images/Lens_and_wavefronts$1.png -resize 114x114 icon114$1.png
convert images/Lens_and_wavefronts$1.png -resize 135x135 icon135$1.png
convert images/Lens_and_wavefronts$1.png -resize 512x512 icon512$1.png
convert images/Lens_and_wavefronts$1.png -resize 144x144 $R/drawable-xxhdpi/icon.png
convert images/Lens_and_wavefronts$1.png -resize 96x96 $R/drawable-xhdpi/icon.png
convert images/Lens_and_wavefronts$1.png -resize 72x72 $R/drawable-hdpi/icon.png
convert images/Lens_and_wavefronts$1.png -resize 72x72 $R/drawable-hdpi/icon.png
convert images/Lens_and_wavefronts$1.png -resize 48x48 $R/drawable-mdpi/icon.png
convert images/Lens_and_wavefronts$1.png -resize 32x32 $R/drawable-ldpi/icon.png
