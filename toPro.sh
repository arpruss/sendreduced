sh scale.sh
mv src/mobi/omegacentauri/SendReduced src/mobi/omegacentauri/SendReduced_pro
for x in src/mobi/omegacentauri/SendReduced_pro/*.java AndroidManifest.xml res/{xml,layout}/*.xml; do
    echo Fixing $x
    sed -i "s/omegacentauri\\.SendReduced/omegacentauri.SendReduced_pro/" $x
done
sed -i "s/android:label=\"Send Reduced Free\"/android:label=\"Send Reduced Pro\"/" AndroidManifest.xml
\