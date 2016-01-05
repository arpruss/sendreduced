sh scale.sh free
mv src/mobi/omegacentauri/SendReduced_pro src/mobi/omegacentauri/SendReduced
for x in src/mobi/omegacentauri/SendReduced/*.java AndroidManifest.xml res/{xml,layout}/*.xml; do
    echo Fixing $x
    sed -i "s/omegacentauri\\.SendReduced_pro/omegacentauri.SendReduced/" $x
done
sed -i "s/android:label=\"Send Reduced Pro\"/android:label=\"Send Reduced Free\"/" AndroidManifest.xml

