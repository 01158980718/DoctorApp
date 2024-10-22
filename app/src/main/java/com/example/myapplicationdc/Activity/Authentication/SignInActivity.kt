package com.example.myapplicationdc.Activity.Authentication

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.example.myapplicationdc.Activity.BaseActivity
import com.example.myapplicationdc.Activity.Doctor_Dashboard
import com.example.myapplicationdc.Activity.MainActivity
import com.example.myapplicationdc.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

class SignInActivity : BaseActivity() {
    private var _binding: ActivitySignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var userType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userType = intent.getStringExtra("userType") ?: "unknown"
        val userEmail = intent.getStringExtra("userEmail")

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        // Set up click listeners
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.btnSignIn.setOnClickListener {
            userLogin()
        }
    }

    // User login with email and password
    private fun userLogin() {
        val email = binding.etSinInEmail.text.toString()
        val password = binding.etSinInPassword.text.toString()

        if (validateForm(email, password)) {
            showProgressBar()
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    hideProgressBar()
                    if (task.isSuccessful) {
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            val userId = currentUser.uid
                            val userEmail = currentUser.email ?: email

                            val user = User(id = userId, email = userEmail, usertype = userType)

                            database.child("users").child(userId).setValue(user).addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    // Fetch ID based on user type
                                    if (userType.equals("Doctor", ignoreCase = true)) {
                                        fetchDoctorIdAndNavigate(userEmail)
                                    } else {
                                        fetchPatientIdAndNavigate(userEmail)
                                    }
                                } else {
                                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Oops! Something went wrong", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    // Fetch the doctor's ID from Firebase using their email, only for Doctor users
    private fun fetchDoctorIdAndNavigate(email: String) {
        val doctorRef = database.child("Doctors")

        // Query to match the email in the "Doctors" reference
        val query = doctorRef.orderByChild("email").equalTo(email)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (doctorSnapshot in snapshot.children) {
                        val doctorId = doctorSnapshot.key // Fetch the doctor's ID
                        if (doctorId != null) {
                            // Navigate to Doctor Dashboard with the doctor ID
                            Log.d("FetchDoctor", "Fetched ID: $doctorId")
                            navigateToDoctorDashboard(doctorId, email)
                        }
                    }
                } else {
                    Toast.makeText(this@SignInActivity, "Doctor not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SignInActivity", "Database error: ${error.message}")
                Toast.makeText(this@SignInActivity, "Failed to fetch doctor data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchPatientIdAndNavigate(email: String) {
        val patientRef = database.child("Patients")

        // Query to match the email in the "Patients" reference
        val query = patientRef.orderByChild("email").equalTo(email)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (patientSnapshot in snapshot.children) {
                        // Fetch the patient's ID from the "id" child
                        val patientId = patientSnapshot.child("id").getValue(Int::class.java) // Change to String if ID is stored as a String
                        val patientName = patientSnapshot.child("pname").getValue(String::class.java) // Fetch the patient's name

                        // Log the fetched values
                        Log.d("FetchPatient", "Fetched ID: $patientId, Name: $patientName for email: $email")

                        if (patientId != null && patientName != null) {
                            // Navigate to MainActivity with the patient ID and name
                            navigateToPatientHome(patientId, patientName, email)
                        } else {
                            Log.e("FetchPatient", "Patient ID or name is null for email: $email")
                        }
                    }
                } else {
                    Toast.makeText(this@SignInActivity, "Patient not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@SignInActivity, "Error fetching patient data: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Navigate to Doctor Dashboard with doctor ID
    private fun navigateToDoctorDashboard(doctorId: String?, email: String) {
        val intent = Intent(this, Doctor_Dashboard::class.java)
        intent.putExtra("doctorId", doctorId) // Pass doctor ID
        intent.putExtra("userEmail", email)   // Pass user email
        startActivity(intent)
        finish()
    }

    // Navigate to MainActivity with patient ID
    private fun navigateToPatientHome(patientId:Int, patientName: String, email: String) {
        // Intent to navigate to the patient home activity
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("patientId", patientId)
        intent.putExtra("PATIENT_NAME", patientName) // Pass the patient's name
        intent.putExtra("EMAIL", email)
        startActivity(intent)
        finish() // Optional: finish the current activity if you don't want to return to it
    }
    private fun validateForm(email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Please enter a valid email"
                false
            }
            TextUtils.isEmpty(password) -> {
                binding.tilPassword.error = "Please enter a password"
                false
            }
            else -> true
        }
    }
}
