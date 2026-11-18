import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FavoritosService } from '../../servicios/favoritos.service';
import { SesionService } from '../../servicios/sesion.service';


@Component({
  selector: 'app-boton-favorito',
  imports: [],
  templateUrl: './boton-favorito.html',
  styleUrl: './boton-favorito.css'
})
export class BotonFavorito implements OnInit {

  private favoritos = inject(FavoritosService);
  sesion = inject(SesionService);

  idPublicacion = input.required<number>();
  guardado = signal(false);

  ngOnInit(): void {
    
    if (this.sesion.usuario()?.rol !== 'HUESPED') {
      return;
    }
    this.favoritos.listar().subscribe({
      next: (lista) => this.guardado.set(lista.publicaciones.includes(this.idPublicacion()))
    });
  }


  cambiar(): void {
    const peticion = this.guardado()
      ? this.favoritos.quitar(this.idPublicacion())
      : this.favoritos.agregar(this.idPublicacion());

    peticion.subscribe({
      next: (lista) => this.guardado.set(lista.publicaciones.includes(this.idPublicacion()))
    });
  }
}