"use client"

import { useState } from "react"
import Image from "next/image"

type ProductGalleryProps = {
  images: string[]
  productName: string
}

export function ProductGallery({ images, productName }: ProductGalleryProps) {
  const [selectedIndex, setSelectedIndex] = useState(0)

  return (
    <div className="flex gap-4">
      {/* Thumbnail list */}
      <div className="flex flex-col gap-2">
        {images.map((image, index) => (
          <button
            key={index}
            onClick={() => setSelectedIndex(index)}
            className={`relative w-20 h-20 border-2 rounded-lg overflow-hidden transition-all ${
              selectedIndex === index ? "border-primary" : "border-gray-200 hover:border-gray-400"
            }`}
          >
            <Image
              src={image || "/placeholder.svg"}
              alt={`${productName} ${index + 1}`}
              fill
              className="object-cover"
            />
          </button>
        ))}
      </div>

      {/* Main image */}
      <div className="relative flex-1 aspect-square rounded-lg overflow-hidden border">
        <Image src={images[selectedIndex] || "/placeholder.svg"} alt={productName} fill className="object-cover" />
      </div>
    </div>
  )
}
