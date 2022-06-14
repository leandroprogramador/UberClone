package com.leandro.uberclone.ui.home

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.leandro.uberclone.R
import com.leandro.uberclone.databinding.FragmentHomeBinding
import com.leandro.uberclone.utils.Common.DRIVERS_LOCATION_REFERENCE

class HomeFragment : Fragment(), OnMapReadyCallback {

    private val REQUEST_LOCATION: Int = 100
    private var _binding: FragmentHomeBinding? = null
    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCall: LocationCallback
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private val binding get() = _binding!!
    private lateinit var onlineRef : DatabaseReference
    private lateinit var currentUserRef : DatabaseReference
    private lateinit var driversLocationRef : DatabaseReference
    private lateinit var geoFire : GeoFire
    private val onlineValueEventListener = object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists()){
                currentUserRef.onDisconnect().removeValue()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        init()
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return root
    }

    @SuppressLint("MissingPermission")
    fun init() {
        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected")
        driversLocationRef = FirebaseDatabase.getInstance().getReference(DRIVERS_LOCATION_REFERENCE)
        currentUserRef = FirebaseDatabase.getInstance().getReference(DRIVERS_LOCATION_REFERENCE).child(
            FirebaseAuth.getInstance().currentUser!!.uid
        )
        geoFire = GeoFire(driversLocationRef)
        registerOnlineSystem()

        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.fastestInterval = 3000
        locationRequest.interval = 5000
        locationRequest.smallestDisplacement = 10f


        locationCall = object : LocationCallback() {
            @SuppressLint("MissingPermission")
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                if (!locationResult.locations.isNullOrEmpty()) {
                    val latLng = LatLng(
                        locationResult.lastLocation.latitude,
                        locationResult.lastLocation.longitude
                    )
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                    geoFire.setLocation(
                        FirebaseAuth.getInstance().currentUser!!.uid,
                        GeoLocation(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
                    ){key : String?, error: DatabaseError ? ->
                        error?.let {
                            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
                        } ?: run {
                            Snackbar.make(mapFragment.requireView(), "You 're online!", Snackbar.LENGTH_LONG).show()
                        }
                    }

                }
            }
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest, locationCall, Looper.myLooper()!!)
        getLocation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        fusedLocationProviderClient!!.removeLocationUpdates(locationCall)
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineRef.removeEventListener(onlineValueEventListener)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_DENIED && ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_DENIED
        ) {

            configureLocationButton()

        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION, ),
                REQUEST_LOCATION
            )

        }

        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.uber_maps_style
                )
            )
            if (!success) {
                Log.e("UBER ERROR", "Style map parsing error")
            }
        } catch (ex: Resources.NotFoundException) {
            Log.e("UBER ERROR", ex.message!!)
        }

    }

    @SuppressLint("MissingPermission")
    private fun configureLocationButton() {
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.setOnMyLocationButtonClickListener {
            getLocation()

            return@setOnMyLocationButtonClickListener true
        }


        val locationButtom = (mapFragment.requireView()
            .findViewById<View>("1".toInt())
            .parent as View).findViewById<View>("2".toInt())
        val params = locationButtom.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.ALIGN_TOP, 0)
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        params.bottomMargin = 50
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        fusedLocationProviderClient!!.lastLocation.addOnFailureListener { e ->
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener { location ->
            val latLng = LatLng(location.latitude, location.longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && requestCode == REQUEST_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(requireContext(), "Permissao DENIED", Toast.LENGTH_SHORT).show()
                requireActivity().finish()
            } else{
                init()
                configureLocationButton()
            }
        }
    }

}