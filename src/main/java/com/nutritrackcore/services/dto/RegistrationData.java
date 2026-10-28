package com.nutritrackcore.services.dto;

import com.nutritrackcore.model.Sex;
import java.time.LocalDate;

/**
 * Datos que escribe una persona en el formulario de registro.
 * <p>
 * Es un objeto de transferencia inmutable: la interfaz lo construye una vez y
 * se lo entrega a {@code AuthenticationService.register}. La contraseña viaja
 * en texto plano únicamente dentro de este objeto; el servicio la convierte en
 * hash y nunca la guarda.
 */
public class RegistrationData {

    /** Nombre completo. */
    private final String name;

    /** Correo electrónico (también sirve para iniciar sesión). */
    private final String email;

    /** Contraseña en texto plano; nunca se persiste. */
    private final String password;

    /** Peso actual en kilogramos. */
    private final double weightKg;

    /** Altura actual en centímetros. */
    private final double heightCm;

    /** Fecha de nacimiento. */
    private final LocalDate birthDate;

    /** Sexo biológico, usado en las fórmulas de IMC y requerimiento calórico. */
    private final Sex sex;

    /**
     * Crea los datos de registro. No valida nada: la validación (en español) la
     * hace {@code AuthenticationService.register}.
     *
     * @param name      nombre completo
     * @param email     correo electrónico
     * @param password  contraseña en texto plano
     * @param weightKg  peso en kilogramos
     * @param heightCm  altura en centímetros
     * @param birthDate fecha de nacimiento
     * @param sex       sexo
     */
    public RegistrationData(String name, String email, String password, double weightKg,
                            double heightCm, LocalDate birthDate, Sex sex) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        this.birthDate = birthDate;
        this.sex = sex;
    }

    /**
     * Devuelve el nombre completo.
     *
     * @return nombre escrito por la persona
     */
    public String getName() {
        return name;
    }

    /**
     * Devuelve el correo electrónico.
     *
     * @return correo escrito por la persona
     */
    public String getEmail() {
        return email;
    }

    /**
     * Devuelve la contraseña en texto plano.
     *
     * @return contraseña escrita por la persona
     */
    public String getPassword() {
        return password;
    }

    /**
     * Devuelve el peso.
     *
     * @return peso en kilogramos
     */
    public double getWeightKg() {
        return weightKg;
    }

    /**
     * Devuelve la altura.
     *
     * @return altura en centímetros
     */
    public double getHeightCm() {
        return heightCm;
    }

    /**
     * Devuelve la fecha de nacimiento.
     *
     * @return fecha de nacimiento
     */
    public LocalDate getBirthDate() {
        return birthDate;
    }

    /**
     * Devuelve el sexo.
     *
     * @return sexo elegido
     */
    public Sex getSex() {
        return sex;
    }

    /**
     * Representación de texto sin la contraseña, para poder imprimir el objeto
     * en registros de depuración sin filtrarla.
     *
     * @return texto con los datos, excepto la contraseña
     */
    @Override
    public String toString() {
        return "RegistrationData{name='" + name + "', email='" + email + "', weightKg=" + weightKg
                + ", heightCm=" + heightCm + ", birthDate=" + birthDate + ", sex=" + sex + "}";
    }
}