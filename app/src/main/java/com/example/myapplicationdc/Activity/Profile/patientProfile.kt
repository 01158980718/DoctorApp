package com.example.myapplicationdc.Activity.Profile

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplicationdc.Domain.PatientModel
import com.example.myapplicationdc.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class patientProfile : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var patientModel: PatientModel

    private lateinit var editPatientName: EditText
    private lateinit var editPatientAge: EditText
    private lateinit var editMedicalHistory: EditText
    private lateinit var editAddress: EditText
    private lateinit var editMobile: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var imagePatient: ImageView
    private lateinit var btnSavePatient: MaterialButton

    private var patientId: Int = 0 // You will pass this ID from the main activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_profile)

        // Initialize views
        editPatientName = findViewById(R.id.editPatientName)
        editPatientAge = findViewById(R.id.editPatientAge)
        editMedicalHistory = findViewById(R.id.editMedicalHistory)
        editAddress = findViewById(R.id.edit_pationt_address)
        editMobile = findViewById(R.id.edit_pationt_Mobile)
        spinnerGender = findViewById(R.id.spinnerGender)
        btnSavePatient = findViewById(R.id.btnSavePatient)

        // Set up the gender spinner
        val genderOptions = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = adapter

        // Get patient ID from intent
        patientId = intent.getIntExtra("patientId", 0)

        // Initialize Firebase reference
        database = FirebaseDatabase.getInstance().getReference("Patients")

        // Fetch patient data and set it in the views
        fetchPatientData()

        // Save button functionality
        btnSavePatient.setOnClickListener {
            showSaveConfirmationDialog()
        }
    }

    private fun fetchPatientData() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Iterate through all patients to find the one with the matching ID
                for (patientSnapshot in snapshot.children) {
                    val patient = patientSnapshot.getValue(PatientModel::class.java)

                    // Check if the patient ID matches
                    if (patient != null && patient.id == patientId) {
                        patientModel = patient

                        // Set data to views
                        editPatientName.setText(patientModel.pname)
                        editPatientAge.setText(patientModel.age.toString())
                        editMedicalHistory.setText(patientModel.medicalHistory)
                        editAddress.setText(patientModel.patient_address)
                        editMobile.setText(patientModel.patient_Mobile.toString())

                        // Set the spinner value for gender
                        val genderPosition = (spinnerGender.adapter as ArrayAdapter<String>).getPosition(patientModel.gender)
                        spinnerGender.setSelection(genderPosition)

                        // Load image with Glide
                        return // Exit once the patient is found
                    }
                }
                Toast.makeText(this@patientProfile, "Patient not found", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@patientProfile, "Failed to load patient data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showSaveConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Update")
            .setMessage("Are you sure you want to update the patient's information?")
            .setPositiveButton("Yes") { dialog, _ ->
                updatePatientData()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updatePatientData() {
        // Prepare updated patient model with new data
        val updatedPatient = PatientModel(
            id = patientId,
            pname = editPatientName.text.toString(),
            age = editPatientAge.text.toString().toIntOrNull() ?: 0,
            gender = spinnerGender.selectedItem.toString(),
            patient_address = editAddress.text.toString(),
            patient_Mobile = editMobile.text.toString().toIntOrNull() ?: 0,
            medicalHistory = editMedicalHistory.text.toString(),

        )

        // Update in Firebase
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Iterate through all patients to find the one with the matching ID
                for (patientSnapshot in snapshot.children) {
                    val patient = patientSnapshot.getValue(PatientModel::class.java)

                    // Check if the patient ID matches
                    if (patient != null && patient.id == patientId) {
                        // Update the patient data in Firebase
                        patientSnapshot.ref.setValue(updatedPatient)

                            .addOnSuccessListener {
                                Toast.makeText(this@patientProfile, "Patient data updated successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { error ->
                                Toast.makeText(this@patientProfile, "Failed to update patient data: ${error.message}", Toast.LENGTH_SHORT).show()
                                Log.e("UpdatePatient", "Error updating patient data$patientSnapshot")
                            }
                        return // Exit once the patient is found and updated
                    }
                }
                Toast.makeText(this@patientProfile, "Patient not found for update", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@patientProfile, "Failed to update patient data", Toast.LENGTH_SHORT).show()
            }
        })
    }
}