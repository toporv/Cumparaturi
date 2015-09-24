package com.example.vili.cumparaturi;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
public class CumparaturiActivity extends Activity {
    Resources res;
    public ArrayAdapter categorieAdapter;
    public ArrayAdapter articolAdapter;
    public ListView mCategorie;
    public ListView mArticol;
    public TextView mCategorieView;
    public TextView mArticolView;
    public Button mAfiseazaButton;
    public Button mStergeButton;
    public List<String> mListaSalvata=new ArrayList<String>();
    public String categorie;
    private SQLiteDatabase baza;
    SQLiteDatabase.CursorFactory cursor;
    public String tabelSelectat="Selectate";
    String Magazine [];
    String Latitudini[];
    String Longitudini[];
    public float latMagazin=0;
    public float longMagazin=0;
    public String numeMagazin;
    public int gasit=0;
    Location location;
    String provider;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cumparaturi);
        res=getResources();
        //implementez monitorizare locatie
        Magazine=res.getStringArray(R.array.Magazin);
        Latitudini=res.getStringArray(R.array.Latitudine);
        Longitudini=res.getStringArray(R.array.Longitudine);
        long timp=25000;
        float dist=25;
        final LocationManager locationManager= (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            provider=LocationManager.GPS_PROVIDER;
            location = locationManager.getLastKnownLocation(String.valueOf(provider));
        }
        if(location==null) {
            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                provider = LocationManager.NETWORK_PROVIDER;
                Location location = locationManager.getLastKnownLocation(String.valueOf(provider));
            }
        }
        final LocationListener locationListener=new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                for(int i=0; i<Magazine.length; i++) {
                    latMagazin=Float.valueOf(Latitudini[i]);
                    longMagazin=Float.valueOf(Longitudini[i]);
                    numeMagazin=Magazine[i];
                    if ((((latMagazin -  0.003)<location.getLatitude()) &&   (location.getLatitude() < latMagazin + 0.003)) &&
                            ((longMagazin - 0.003) < location.getLongitude() &&(location.getLongitude()<longMagazin + 0.003)))  {
                        if(gasit==0) {
                            Toast.makeText(CumparaturiActivity.this, "Magazin " + numeMagazin + " in apropiere", Toast.LENGTH_LONG).show();
                            gasit= 1;
                            break;
                        }
                    }
                    else {
                        gasit=0;
                    }
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
                Toast.makeText(CumparaturiActivity.this,"Retea disponibila", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProviderDisabled(String provider) {
                    Toast.makeText(CumparaturiActivity.this, "Retea indisponibila", Toast.LENGTH_LONG).show();
            }
        };

            locationManager.requestLocationUpdates(provider, timp, dist, locationListener);
           if(gasit==1){
                locationManager.removeUpdates(locationListener);
            }

        //creez sau deschid baza de date
        baza=openOrCreateDatabase("bazaArticole", 0, cursor);
        //Initializez obiectele grafice
        mAfiseazaButton=(Button)findViewById(R.id.afiseaza);
        mStergeButton=(Button)findViewById(R.id.sterge);
        mStergeButton.setEnabled(false);
        mCategorieView=(TextView)findViewById(R.id.categorieTextView);
        mArticolView=(TextView)findViewById(R.id.articolTextView);
        mCategorie=(ListView)findViewById(R.id.categorie);
        mArticol=(ListView)findViewById(R.id.articol);

        //populez listview mCategorie

        String categorii[]=res.getStringArray(R.array.Categorii);
        String raioane[]=res.getStringArray(R.array.Raioane);
        categorieAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, categorii);
        mCategorie.setAdapter(categorieAdapter);
        //creez tabelul in care vor fi stocate articolele selectate
        //baza.execSQL("DROP TABLE IF EXISTS Selectate");
        baza.execSQL("CREATE TABLE IF NOT EXISTS Selectate (Raion integer, Articol text not null)");
        //creez tabele cu articole pt fiecare categorie
        for(int i=0; i<categorii.length; i++) {
           // baza.execSQL("DROP TABLE IF EXISTS " + categorii[i]);
            creatTabel(raioane[i], categorii[i]);
        }
        //afisez lista cu articolele salvate anterior
        listaSalvata();

        mAfiseazaButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
               //afisez lista cu articolele salvate anterior
               tabelSelectat="Selectate";
                listaSalvata();
            }
        });

        mStergeButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                //sterge lista cu articolele salvate anterior
                stergeLista();
            }
        });

        mCategorie.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //incarca lista mArticole cu articolele din categoria selectata
                categorie=categorieAdapter.getItem(position).toString();
                tabelSelectat=categorie;
                mStergeButton.setEnabled(false);
                incarcaLista(categorie);
                articoleSelectate();
            }
        });

        mArticol.setOnItemClickListener(new AdapterView.OnItemClickListener(){
           @Override
           public void onItemClick(AdapterView<?> parent, View view, int position, long id){
               //adauga articolul selectat sau il stereg pe cel deselectat
               String articol=articolAdapter.getItem(position).toString();
               boolean selectat=mArticol.isItemChecked(position);
               salveazaLista(tabelSelectat, selectat, articol);
           }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cumparaturi, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void incarcaLista(String mCategorie){
        //incarca lista mArticol cu articolele din categoria selectata
        mArticolView.setText(mCategorie);
        int getIdNume=getIdTabelNume(mCategorie);
        String articole[]=res.getStringArray(getIdNume);
        articolAdapter=new ArrayAdapter(this, android.R.layout.simple_list_item_checked, articole);
        mArticol.setAdapter(articolAdapter);
    }

    public void salveazaLista(String tabelPasat, boolean selectat, String articol){
        //modifica tabelul Selectate, adaugand sau stergand - dupa caz- articolul selectat/deselectat
        boolean exista=false;
        Cursor iteratorSelectate;
        Cursor iteratorTabelPasat;
        ContentValues valori=new ContentValues();
        iteratorSelectate=baza.rawQuery("SELECT * FROM Selectate", null);
        iteratorTabelPasat=baza.rawQuery("SELECT * FROM " + tabelPasat, null);
        String numeSelectat=articol;
        String raion;
        if(selectat==true)
        {
            if(iteratorTabelPasat.getCount()>0)
            {
                iteratorTabelPasat.moveToFirst();
                for(int i=0; i<iteratorTabelPasat.getCount();i++){
                    if(articol.equals(iteratorTabelPasat.getString(1))){
                        raion=String.valueOf(iteratorTabelPasat.getInt(0));
                        valori.put("Raion", raion);
                        valori.put("Articol", articol);
                        baza.insert("Selectate", null, valori);
                        break;
                    }
                    iteratorTabelPasat.moveToNext();
                }
        }

        }
        else{
            if(iteratorSelectate.getCount()>0){
              iteratorSelectate.moveToFirst();
                for(int i=0;i<iteratorSelectate.getCount();i++){
                   if(articol.equals(iteratorSelectate.getString(1))){
                       baza.delete("Selectate", "Articol='"  + articol + "'", null);
                       break;
                   }
                    iteratorSelectate.moveToNext();
                }
            }
        }
    }

    public void listaSalvata(){
        //incarca in lista mArticole articolele salvate, aflate in tabelul Selectate
        mArticolView.setText(R.string.cumparaturi);
        Cursor iteratorSalvate;
        String localNume;
        mListaSalvata.clear();
        iteratorSalvate=baza.rawQuery("SELECT * FROM Selectate ORDER BY Raion ASC", null);
        if(iteratorSalvate.getCount()>0) {
            iteratorSalvate.moveToFirst();
            //mListaSalvata.clear();
            for(int i=0; i<iteratorSalvate.getCount(); i++){
                localNume=iteratorSalvate.getString(1);
                mListaSalvata.add(localNume);
                iteratorSalvate.moveToNext();
            }
            mStergeButton.setEnabled(true);
        }
        articolAdapter=new ArrayAdapter(this, android.R.layout.simple_list_item_checked, mListaSalvata);
        mArticol.setAdapter(articolAdapter);
        for (int i=0; i< iteratorSalvate.getCount(); i++)
            mArticol.setItemChecked(i, true);
    }
    public void stergeLista(){
        //este evident, nu??
        if(articolAdapter.getCount()>0){
           articolAdapter.clear();
        }
        baza.execSQL("DROP TABLE IF EXISTS Selectate");
        baza.execSQL("CREATE TABLE IF NOT EXISTS Selectate (Raion integer, Articol text not null)");
        listaSalvata();
        mStergeButton.setEnabled(false);
    }

    public void articoleSelectate() {
        //seteaza isChecked pe true pentru articolele care sunt deja in lista de cumparaturi
        Cursor iteratorSelectate;
        iteratorSelectate=baza.rawQuery("SELECT * FROM Selectate", null);
        String articolSalvat;
        String articolLista;
        if(iteratorSelectate.getCount()>0)
        {
            iteratorSelectate.moveToFirst();
            for(int m=0; m<iteratorSelectate.getCount(); m++) {
                boolean exista=false;
                articolSalvat=iteratorSelectate.getString(1);
                for (int i = 0; i < articolAdapter.getCount(); i++) {
                    articolLista=articolAdapter.getItem(i).toString();
                    if(articolLista.equals(articolSalvat)){
                        mArticol.setItemChecked(i, true);
                        exista=true;
                    }
                    if(exista==true)
                        break;
                }
                iteratorSelectate.moveToNext();
            }
        }
    }

    public void creatTabel(String raion, String categorie){
        //populeaza baza de date cu tabelele necesare
        Cursor iteratorCreare;
        baza.execSQL("CREATE TABLE IF NOT EXISTS " + categorie + " (Raion integer, Articol text not null)");
        iteratorCreare=baza.rawQuery("SELECT * FROM " + categorie, null);
        if(cursor !=null && iteratorCreare.getCount()>0)
        {
            return;
        }
        else
        {
            populeazaTabel(raion, categorie);
        }
    }
    public void populeazaTabel(String raion, String categorie){
        //incarca in tabele articoelle aflate in array-urile din res/strings
        int idNume=getIdTabelNume(categorie);
        int idRaion=getIdTabelRaion(raion);
        ContentValues valori= new ContentValues();
        String  localRaion[]=res.getStringArray(idRaion);
        String localNume[]=res.getStringArray(idNume);
        for(int i=0; i< localNume.length; i++) {
            valori.put("Raion", Integer.valueOf(localRaion[i]));
            valori.put("Articol", localNume[i]);
            baza.insert(categorie, null, valori);
        }

    }
    public int getIdTabelNume(String categorie)
    {
        int getId;
        switch (categorie)
        {
            case "Categorii":
                getId=R.array.Categorii;
                break;
            case "Uzuale":
                getId=R.array.Uzuale;
                break;
            case "Bauturi":
                getId=R.array.Bauturi;
                break;
            case "Cafea":
                getId=R.array.Cafea;
                break;
            case "Carne":
                getId=R.array.Carne;
                break;
            case "Condimente":
                getId=R.array.Condimente;
                break;
            case "Congelate":
                getId=R.array.Congelate;
                break;
            case "Conserve":
                getId=R.array.Conserve;
                break;
            case "Copii":
                getId=R.array.Copii;
                break;
            case "Dulciuri":
                getId=R.array.Dulciuri;
                break;
            case "Fructe":
                getId=R.array.Fructe;
                break;
            case "Lactate":
                getId=R.array.Lactate;
                break;
            case "Legume":
                getId=R.array.Legume;
                break;
            case "Panificatie":
                getId=R.array.Panificatie;
                break;
            case "Saratele":
                getId=R.array.Saratele;
                break;
            case "Semipreparate":
                getId=R.array.Semipreparate;
                break;
            case "Auto":
                getId=R.array.Auto;
                break;
            case "Chimice":
                getId=R.array.Chimice;
                break;
            case "Cosmetice":
                getId=R.array.Cosmetice;
                break;
            case "Electrice":
                getId=R.array.Electrice;
                break;
            case "Incaltaminte":
                getId=R.array.Incaltaminte;
                break;
            case "Menaj":
                getId=R.array.Menaj;
                break;
            case "Papetarie":
                getId=R.array.Papetarie;
                break;
            case "Petshop":
                getId=R.array.Petshop;
                break;
            case "Scule":
                getId=R.array.Scule;
                break;
            case "Textile":
                getId=R.array.Textile;
                break;
            default:
                getId=R.array.Uzuale;
                break;
        }
        return getId;
    }
    public int getIdTabelRaion(String raion)
    {
        int getId;
        switch (raion)
        {
            case "raionCategorii":
                getId=R.array.raionCategorii;
                break;
            case "raionUzuale":
                getId=R.array.raionUzuale;
                break;
            case "raionBauturi":
                getId=R.array.raionBauturi;
                break;
            case "raionCafea":
                getId=R.array.raionCafea;
                break;
            case "raionCarne":
                getId=R.array.raionCarne;
                break;
            case "raionCondimente":
                getId=R.array.raionCondimente;
                break;
            case "raionCongelate":
                getId=R.array.raionCongelate;
                break;
            case "raionConserve":
                getId=R.array.raionConserve;
                break;
            case "raionCopii":
                getId=R.array.raionCopii;
                break;
            case "raionDulciuri":
                getId=R.array.raionDulciuri;
                break;
            case "raionFructe":
                getId=R.array.raionFructe;
                break;
            case "raionLactate":
                getId=R.array.raionLactate;
                break;
            case "raionLegume":
                getId=R.array.raionLegume;
                break;
            case "raionPanificatie":
                getId=R.array.raionPanificatie;
                break;
            case "raionSaratele":
                getId=R.array.raionSaratele;
                break;
            case "raionSemipreparate":
                getId=R.array.raionSemipreparate;
                break;
            case "raionAuto":
                getId=R.array.raionAuto;
                break;
            case "raionChimice":
                getId=R.array.raionChimice;
                break;
            case "raionCosmetice":
                getId=R.array.raionCosmetice;
                break;
            case "raionElectrice":
                getId=R.array.raionElectrice;
                break;
            case "raionIncaltaminte":
                getId=R.array.raionIncaltaminte;
                break;
            case "raionMenaj":
                getId=R.array.raionMenaj;
                break;
            case "raionPapetarie":
                getId=R.array.raionPapetarie;
                break;
            case "raionPetshop":
                getId=R.array.raionPetshop;
                break;
            case "raionScule":
                getId=R.array.raionScule;
                break;
            case "raionTextile":
                getId=R.array.raionTextile;
                break;
            default:
                getId=R.array.raionUzuale;
                break;
        }
        return getId;
    }

}

