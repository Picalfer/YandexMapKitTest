package com.sport.testtaskyamapkit

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sport.testtaskyamapkit.databinding.ActivityMainBinding
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouterType
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationManager
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapView: MapView
    private lateinit var map: Map
    private lateinit var routesCollection: MapObjectCollection
    private lateinit var locationManager: LocationManager

    private var routes = emptyList<DrivingRoute>()
        set(value) {
            field = value
            onRoutesUpdated()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }

        getPermissions()
        locationManager = MapKitFactory.getInstance().createLocationManager()

        mapView = binding.mapview
        map = mapView.mapWindow.map
        map.isRotateGesturesEnabled = false

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        routesCollection = map.mapObjects.addCollection()

        binding.btnUpdate.setOnClickListener {
            locationManager.requestSingleUpdate(object : LocationListener {
                override fun onLocationUpdated(location: Location) {
                    val currentPosition =
                        Point(location.position.latitude, location.position.longitude)
                    buildRoute(currentPosition, GOAL_POINT)
                }

                override fun onLocationStatusUpdated(locationStatus: LocationStatus) {}
            })

        }
    }

    private fun buildRoute(startPoint: Point, goalPoint: Point) {
        setPlacemarks(startPoint, goalPoint)
        moveCamera(startPoint)

        val drivingRouterListener = object : DrivingSession.DrivingRouteListener {
            override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {
                Toast.makeText(this@MainActivity, "Успешно построен маршрут", Toast.LENGTH_SHORT)
                    .show()
                routes = drivingRoutes
            }

            override fun onDrivingRoutesError(error: Error) {
                when (error) {
                    is NetworkError -> Toast.makeText(
                        this@MainActivity,
                        "Routes request error due network issues",
                        Toast.LENGTH_SHORT
                    )
                        .show()

                    else -> Toast.makeText(
                        this@MainActivity,
                        "Routes request unknown error",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }

        val drivingRouter =
            DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.COMBINED)
        val drivingOptions = DrivingOptions().apply {
            routesCount = 1
        }
        val vehicleOptions = VehicleOptions()
        val points = getRoutePoints(startPoint, goalPoint)

        val drivingSession = drivingRouter.requestRoutes(
            points,
            drivingOptions,
            vehicleOptions,
            drivingRouterListener
        )
    }

    private fun getRoutePoints(startPoint: Point, goalPoint: Point): List<RequestPoint> {
        return buildList {
            add(RequestPoint(startPoint, RequestPointType.WAYPOINT, null, null))
            add(RequestPoint(goalPoint, RequestPointType.WAYPOINT, null, null))
        }
    }

    private fun moveCamera(point: Point) {
        map.move(CameraPosition(point, 14.0f, 0.0f, 0.0f))
    }

    private fun setPlacemarks(startPoint: Point, goalPoint: Point) {
        val meImageProvider = ImageProvider.fromResource(this, R.drawable.me_placemark)
        val me_placemark = mapView.mapWindow.map.mapObjects.addPlacemark().apply {
            geometry = startPoint
            setIcon(meImageProvider)
        }

        val mePlacemarkTapListener = MapObjectTapListener { _, point ->
            Toast.makeText(
                this@MainActivity,
                "Вы здесь",
                Toast.LENGTH_SHORT
            ).show()
            true
        }

        me_placemark.addTapListener(mePlacemarkTapListener)


        val goalImageProvider = ImageProvider.fromResource(this, R.drawable.goal_placemark)
        val goal_placemark = mapView.mapWindow.map.mapObjects.addPlacemark().apply {
            geometry = goalPoint
            setIcon(goalImageProvider)
        }

        val goalPlacemarkTapListener = MapObjectTapListener { _, point ->
            Toast.makeText(
                this@MainActivity,
                "Точка прибытия",
                Toast.LENGTH_SHORT
            ).show()
            true
        }

        goal_placemark.addTapListener(goalPlacemarkTapListener)
    }


    private fun getPermissions() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    false
                ) -> {
                    // Precise location access granted.
                }

                permissions.getOrDefault(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    false
                ) -> {
                    // Only approximate location access granted.
                }

                else -> {
                    // No location access granted.
                }
            }
        }

// ...

// Before you perform the actual permission request, check whether your app
// already has the permissions, and whether your app needs to show a permission
// rationale dialog. For more details, see Request permissions.
        locationPermissionRequest.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    private fun PolylineMapObject.styleMainRoute() {
        zIndex = 10f
        setStrokeColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        strokeWidth = 5f
        outlineColor = ContextCompat.getColor(this@MainActivity, R.color.black)
        outlineWidth = 3f
    }

    private fun PolylineMapObject.styleAlternativeRoute() {
        zIndex = 5f
        setStrokeColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        strokeWidth = 4f
        outlineColor = ContextCompat.getColor(this@MainActivity, R.color.black)
        outlineWidth = 2f
    }

    private fun onRoutesUpdated() {
        routesCollection.clear()
        if (routes.isEmpty()) return

        routes.forEachIndexed { index, route ->
            routesCollection.addPolyline(route.geometry).apply {
                if (index == 0) styleMainRoute() else styleAlternativeRoute()
            }
        }
    }

    companion object {
        private val GOAL_POINT = Point(56.833742, 60.635716)
    }
}